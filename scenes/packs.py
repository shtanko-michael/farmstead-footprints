"""Resource-pack extensibility: a test pack adds a star shape and rules (pigs leave stars or paws at random, stone takes
prints). The pack is written into the dev client's resourcepacks folder and switched on in options.txt before
the client starts.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge packs
"""
import json
import math
import re

from PIL import Image, ImageDraw

Y = 100
PACK = "footprints-test-pack"


def prepare(client_dir, pack_format=34):
    root = client_dir / "resourcepacks" / PACK
    shapes = root / "assets" / "footprintstest" / "textures" / "farmstead_footprints" / "shapes"
    rules = root / "assets" / "footprintstest" / "farmstead_footprints" / "profiles"
    shapes.mkdir(parents=True, exist_ok=True)
    rules.mkdir(parents=True, exist_ok=True)
    (root / "pack.mcmeta").write_text(json.dumps({"pack": {"pack_format": pack_format, "description": "Footprints test"}}),
                                      encoding="utf-8")
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pts = []
    for i in range(10):
        r = 58 if i % 2 == 0 else 24
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((64 + r * math.cos(a), 64 + r * math.sin(a)))
    d.polygon(pts, fill=(40, 30, 20, 255))
    img.save(shapes / "star.png")
    (rules / "test.json").write_text(json.dumps({
        "profiles": ["minecraft:pig = shape:star|paw size:1.3"],
        "surfaces": {"include": ["minecraft:smooth_stone"]},
    }), encoding="utf-8")
    opts = client_dir / "options.txt"
    text = opts.read_text(encoding="utf-8") if opts.exists() else ""
    line = f'resourcePacks:["vanilla","file/{PACK}"]'
    text = re.sub(r"(?m)^resourcePacks:.*$", line, text) if "resourcePacks:" in text else text + line + "\n"
    opts.write_text(text, encoding="utf-8")


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} -2 {Y + 1} 0")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 1} -4 20 {Y - 1} 12 stone", f"fill -6 {Y} -4 20 {Y + 5} 12 air",
             f"fill -6 {Y} -4 20 {Y} 12 smooth_stone", f"fill 0 {Y} 4 14 {Y} 6 sand", "kill @e[type=!player]")
    ctx.wait(1)
    for z in (1.5, 5.5):
        ctx.cmd(f"tp {p} 7 {Y + 1} {z - 2.5}")
        ctx.cmd(f'summon pig 0.5 {Y + 1} {z} {{Tags:["w"],NoAI:1b}}')
        ctx.wait(0.4)
        ctx.move_entity("@e[tag=w,limit=1]", (0.5, z), (13.5, z), Y + 1, 3.0)
        ctx.cmd("kill @e[tag=w]")
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 7 {Y + 4.5} 3.5 0 90")
    ctx.wait(1.2)
    ctx.shot("packs-pigs")
