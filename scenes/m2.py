"""M2 check: prints on grass, wool and moss (hardness rule), none on stone; a player's own PNG mapped to pigs in
the config; hopping rabbits; a spider; the per-mob rules and the atlas rebuild through /footprints reload.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge m2
"""
import re

Y = 100
LANES = [("grass_block", 0), ("white_wool", 5), ("moss_block", 10), ("sand", 15), ("stone", 20)]


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def star_png(path):
    """A five-pointed star, dark on white: stands for 'a player's own picture'."""
    import math
    from PIL import Image, ImageDraw
    im = Image.new("RGB", (200, 220), "white")
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        r = 95 if i % 2 == 0 else 40
        pts.append((100 + r * math.cos(a), 115 + r * math.sin(a)))
    ImageDraw.Draw(im).polygon(pts, fill="black")
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             "gamerule doMobSpawning false", "kill @e[type=!player]", f"gamemode spectator {p}", f"tp {p} 20 {Y + 6} 10")
    ctx.wait(3)
    ctx.cmds(f"fill -2 {Y - 1} -6 44 {Y - 1} 26 stone", f"fill -2 {Y + 1} -6 44 {Y + 8} 26 air")
    for block, z in LANES:
        ctx.cmd(f"fill 0 {Y} {z} 40 {Y} {z + 3} {block}")

    # the player's own picture, mapped to pigs in the config, picked up by a reload
    star_png(ctx.client_dir / "config" / "farmstead_footprints" / "textures" / "star.png")
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*profiles = .*$", 'profiles = ["minecraft:pig = shape:star size:1.3"]', text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(2)

    # walkers: a villager on grass, a cow on wool, a pig (stars) on moss, a horse on sand, a zombie on stone
    ctx.cmds("kill @e[type=!player]",
             f"summon villager 1.5 {Y + 1} 1.5 {{NoAI:1b}}", f"summon cow 1.5 {Y + 1} 6.5 {{NoAI:1b}}",
             f"summon pig 1.5 {Y + 1} 11.5 {{NoAI:1b}}", f"summon spider 1.5 {Y + 1} 16.5 {{NoAI:1b}}",
             f"summon zombie 1.5 {Y + 1} 21.5 {{NoAI:1b}}")
    ctx.wait(1)
    ctx.move_entity("@e[type=villager,limit=1]", (1.5, 1.5), (30.5, 1.5), Y + 1, 3)
    ctx.move_entity("@e[type=cow,limit=1]", (1.5, 6.5), (30.5, 6.5), Y + 1, 3)
    ctx.move_entity("@e[type=pig,limit=1]", (1.5, 11.5), (30.5, 11.5), Y + 1, 3)
    ctx.move_entity("@e[type=spider,limit=1]", (1.5, 16.5), (30.5, 16.5), Y + 1, 4)
    ctx.move_entity("@e[type=zombie,limit=1]", (1.5, 21.5), (30.5, 21.5), Y + 1, 3)
    # rabbits hop about on the sand on their own
    ctx.cmds(*[f"summon rabbit {34 + i}.5 {Y + 1} 16.5" for i in range(3)])
    ctx.wait(25)

    ctx.cmd(f"tp {p} 16 {Y + 14} -8 0 52")
    ctx.wait(2)
    ctx.shot("m2-overview")
    ctx.cmd(f"tp {p} 8 {Y + 4.5} 4 -90 60")
    ctx.wait(2)
    ctx.shot("m2-grass-wool-moss")
    ctx.cmd(f"tp {p} 36 {Y + 4} 13 0 70")
    ctx.wait(2)
    ctx.shot("m2-rabbits")
