"""Placeholder footprint masks (4x4 grid of 32x32 cells, white + alpha).

Final art comes from the Codex texture pipeline (farmstead-mod-tools/assets); these keep
the mod testable until then. Run: python -I tools/gen_placeholder_footprints.py
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

CELL = 32
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/imprint/textures/footprints.png"


def boot(d, mirror):
    # sole: wide forefoot, narrow waist, round heel; toe at the top (v0)
    d.ellipse((9, 2, 23, 17), fill=255)
    d.rectangle((11, 12, 21, 22), fill=255)
    d.ellipse((10, 19, 22, 30), fill=255)
    for y in range(4, 30, 3):  # tread lines
        d.line((9, y, 23, y), fill=150)


def bare(d, mirror):
    d.ellipse((10, 9, 22, 21), fill=255)
    d.ellipse((11, 18, 21, 30), fill=230)
    toes = [(9, 2, 13, 7), (13, 1, 17, 6), (17, 2, 20, 6), (20, 4, 23, 8)]
    for t in toes:
        d.ellipse(t, fill=235)


def paw(d, mirror):
    d.ellipse((10, 14, 22, 26), fill=255)
    for t in [(6, 8, 12, 14), (11, 4, 16, 10), (16, 4, 21, 10), (20, 8, 26, 14)]:
        d.ellipse(t, fill=240)


def hoof(d, mirror):
    d.ellipse((7, 5, 25, 27), fill=255)
    d.ellipse((12, 12, 20, 29), fill=0)
    d.line((16, 6, 16, 18), fill=60, width=2)


def bird(d, mirror):
    for end in [(6, 4), (16, 2), (26, 4)]:
        d.line((16, 22) + end, fill=255, width=3)
    d.line((16, 22, 16, 30), fill=230, width=3)


def big(d, mirror):
    d.ellipse((5, 9, 27, 31), fill=255)
    for t in [(5, 2, 12, 9), (12, 0, 20, 7), (20, 2, 27, 9)]:
        d.ellipse(t, fill=240)


SHAPES = [(boot, False), (boot, True), (bare, False), (bare, True), (paw, False),
          (hoof, False), (bird, False), (big, False), (big, True)]


def cell(draw_fn, mirror, seed):
    rnd = random.Random(seed)
    mask = Image.new("L", (CELL, CELL), 0)
    draw_fn(ImageDraw.Draw(mask), mirror)
    mask = mask.filter(ImageFilter.GaussianBlur(0.6))
    px = mask.load()
    for y in range(CELL):
        for x in range(CELL):
            if px[x, y]:
                px[x, y] = max(0, min(255, int(px[x, y] * rnd.uniform(0.82, 1.0))))
    if mirror:
        mask = mask.transpose(Image.FLIP_LEFT_RIGHT)
    # Pressed-in look: the mod tints prints slightly brighter than the ground, the texture
    # darkens the floor of the print and leaves a bright rim where the edge is pushed up.
    inner = mask.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.8))
    shade = Image.new("L", (CELL, CELL))
    sp, mp, ip = shade.load(), mask.load(), inner.load()
    for y in range(CELL):
        for x in range(CELL):
            depth = ip[x, y] / 255.0              # 0 at the rim, 1 deep inside
            sp[x, y] = int(255 * (1.0 - 0.62 * depth))
    img = Image.merge("RGBA", (shade, shade, shade, mask))
    return img


def main():
    atlas = Image.new("RGBA", (CELL * 4, CELL * 4), (255, 255, 255, 0))
    for i, (fn, mirror) in enumerate(SHAPES):
        atlas.paste(cell(fn, mirror, i), ((i % 4) * CELL, (i // 4) * CELL))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
