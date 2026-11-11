"""Slice the Codex footprint sheet (art/footprints-raw-*.png, 4x3 prints on white) into the mod atlases.

    python -I tools/build_footprints.py art/footprints-raw-1.png

A print is drawn as two translucent layers over the untouched ground texture, so sand grains and
snow sparkle stay visible inside it — a pressed-in hollow, not a painted patch:

  footprints_shadow.png  white RGB, alpha = how much the hollow darkens the ground. Deeper parts of
                         the Codex drawing (darker ink) give more shadow; the inner wall right below
                         the edge gets a little extra occlusion; fine grain breaks up flat areas.
  footprints_rim.png     white RGB, alpha = the displaced lip just outside the print, lit lighter.

Both are 4x4 cells of 64 px in the same layout. The mod tints the shadow with a per-surface shadow
colour (bluish on snow, warm on sand) and the rim with a lifted ground colour.
Prints each mask's width/length ratio, which FootShape uses so prints are never stretched.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/farmstead_footprints/textures"
CELL = 64
FILL = 0.80          # share of the cell the longer side of a print takes (room left for the rim)
NOISE = 18           # ink below this is paper, not print

# sheet cell (row-major, 4 per row) -> atlas cell
#   sheet: 0 boot L, 1 boot R, 2 bare L, 3 bare R, 4 wolf, 5 cat, 6 horse, 7 cloven,
#          8 bird, 9 golem, 10 bear, 11 sneak
LAYOUT = {0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 6: 5, 8: 6, 9: 7, 5: 9, 7: 10, 10: 11, 11: 12}
NAMES = {0: "BOOT_LEFT", 1: "BOOT_RIGHT", 2: "BARE_LEFT", 3: "BARE_RIGHT", 4: "PAW", 5: "HOOF", 6: "BIRD",
         7: "BIG_LEFT", 8: "BIG_RIGHT", 9: "CAT", 10: "CLOVEN", 11: "BEAR", 12: "SNEAK"}


def ink_of(img: Image.Image) -> Image.Image:
    ink = ImageOps.invert(ImageOps.grayscale(img.convert("RGB")))
    return ink.point(lambda v: 0 if v < NOISE else v)


def cut(ink: Image.Image, index: int, cols: int = 4, rows: int = 3) -> Image.Image:
    col, row = index % cols, index // cols
    w, h = ink.size
    cw, ch = w / cols, h / rows
    cell = ink.crop((int(col * cw), int(row * ch), int((col + 1) * cw), int((row + 1) * ch)))
    bbox = cell.getbbox()
    if not bbox:
        raise SystemExit(f"sheet cell {index} is empty")
    return cell.crop(bbox)


def left_heavy(ink: Image.Image) -> bool:
    """True if the toe end (top third) leans left — a RIGHT foot seen from above."""
    w, h = ink.size
    top = ink.crop((0, 0, w, max(1, h // 3)))
    px = top.load()
    total = mx = 0
    for y in range(top.size[1]):
        for x in range(w):
            total += px[x, y]
            mx += px[x, y] * x
    return total > 0 and mx / total < w / 2


def fit(print_ink: Image.Image) -> tuple[Image.Image, float]:
    pw, ph = print_ink.size
    scale = FILL * CELL / max(pw, ph)
    nw, nh = max(1, round(pw * scale)), max(1, round(ph * scale))
    ink = Image.new("L", (CELL, CELL), 0)
    ink.paste(print_ink.resize((nw, nh), Image.LANCZOS), ((CELL - nw) // 2, (CELL - nh) // 2))
    return ink, pw / ph


def layers(ink: Image.Image, seed: int) -> tuple[Image.Image, Image.Image]:
    rnd = random.Random(seed)
    mask = ink.point(lambda v: 255 if v > 40 else 0).filter(ImageFilter.GaussianBlur(0.7))
    # inner wall occlusion: a band just inside the edge
    eroded = mask.filter(ImageFilter.MinFilter(5))
    wall = ImageChops.subtract(mask, eroded).filter(ImageFilter.GaussianBlur(1.0))
    # displaced lip just outside the edge
    dilated = mask.filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.GaussianBlur(1.6))
    lip = ImageChops.subtract(dilated, mask.filter(ImageFilter.MaxFilter(3)))

    shadow = Image.new("L", (CELL, CELL))
    rim = Image.new("L", (CELL, CELL))
    sp, rp = shadow.load(), rim.load()
    ip, mp, wp, lp = ink.load(), mask.load(), wall.load(), lip.load()
    for y in range(CELL):
        for x in range(CELL):
            inside = mp[x, y] / 255.0
            depth = ip[x, y] / 255.0
            grain = rnd.uniform(-0.06, 0.06)
            a = inside * (0.28 + 0.62 * depth + grain) + 0.22 * wp[x, y] / 255.0
            sp[x, y] = max(0, min(255, int(a * 255)))
            r = lp[x, y] / 255.0 * (0.55 + rnd.uniform(-0.15, 0.15))
            rp[x, y] = max(0, min(255, int(r * 255)))
    white = Image.new("L", (CELL, CELL), 255)
    return Image.merge("RGBA", (white, white, white, shadow)), Image.merge("RGBA", (white, white, white, rim))


def main(sheet_path: str) -> None:
    ink = ink_of(Image.open(sheet_path))
    prints = {i: cut(ink, i) for i in range(12)}
    for left, right in ((0, 1), (2, 3)):
        if left_heavy(prints[left]) and not left_heavy(prints[right]):
            prints[left], prints[right] = prints[right], prints[left]

    shadow_atlas = Image.new("RGBA", (CELL * 4, CELL * 4), (255, 255, 255, 0))
    rim_atlas = Image.new("RGBA", (CELL * 4, CELL * 4), (255, 255, 255, 0))
    aspects = {}
    for sheet_cell, atlas_cell in LAYOUT.items():
        fitted, aspect = fit(prints[sheet_cell])
        shadow, rim = layers(fitted, seed=atlas_cell)
        pos = ((atlas_cell % 4) * CELL, (atlas_cell // 4) * CELL)
        shadow_atlas.paste(shadow, pos)
        rim_atlas.paste(rim, pos)
        aspects[atlas_cell] = aspect
    # giant right foot = mirrored left
    for atlas in (shadow_atlas, rim_atlas):
        big = atlas.crop((3 * CELL, CELL, 4 * CELL, 2 * CELL)).transpose(Image.FLIP_LEFT_RIGHT)
        atlas.paste(big, (0, 2 * CELL))
    aspects[8] = aspects[7]

    TEX.mkdir(parents=True, exist_ok=True)
    shadow_atlas.save(TEX / "footprints_shadow.png")
    rim_atlas.save(TEX / "footprints_rim.png")
    old = TEX / "footprints.png"
    if old.exists():
        old.unlink()
    print(TEX / "footprints_shadow.png")
    print(TEX / "footprints_rim.png")
    for cell in sorted(aspects):
        print(f"  {cell:2d} {NAMES[cell]:<11} width/length = {aspects[cell]:.2f}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else str(ROOT / "art/footprints-raw-1.png"))
