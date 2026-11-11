"""Detailed vs pixel style on the same prints: walk once, photograph every lane in both styles (the style switches
live, prints keep their data for both).

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge style
"""
import re

Y = 100
LANES = [("snow_block", 0), ("snow", 4), ("sand", 8), ("grass_block", 12), ("mud", 16), ("gravel", 20)]
WALKERS = [("player", 0.8), ("horse", 2.0), ("wolf", 3.0)]


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def style(ctx, value, decay="CRUMBLE"):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*style = .*$", f'style = "{value}"', text)
    text = re.sub(r"(?m)^#?\s*decay = .*$", f'decay = "{decay}"', text)
    text = re.sub(r"(?m)^#?\s*lifetimeMultiplier = .*$", "lifetimeMultiplier = 1.0", text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(1.5)


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} -2 {Y + 1} 0")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 1} -4 20 {Y - 1} 26 stone", f"fill -6 {Y} -4 20 {Y + 5} 26 air",
             f"fill -6 {Y} -4 20 {Y} 26 smooth_stone", "kill @e[type=!player]")
    for block, z in LANES:
        if block == "snow":
            ctx.cmds(f"fill 0 {Y} {z} 14 {Y} {z + 3} dirt", f"fill 0 {Y + 1} {z} 14 {Y + 1} {z + 3} snow[layers=2]")
        else:
            ctx.cmd(f"fill 0 {Y} {z} 14 {Y} {z + 3} {block}")
    style(ctx, "DETAILED")
    hook(ctx, "prints.clear")
    ctx.wait(1)
    for block, z in LANES:
        y = Y + 1 + (0.25 if block == "snow" else 0)
        for who, lane in WALKERS:
            if who == "player":
                ctx.cmd(f"item replace entity {p} armor.feet with iron_boots")
                ctx.walk((0.5, z + lane), (13.5, z + lane), y, speed=3.2)
                ctx.cmd(f"item replace entity {p} armor.feet with air")
            else:
                ctx.cmd(f"tp {p} 7 {Y + 1} {z - 1}")
                ctx.cmd(f'summon {who} 0.5 {y} {z + lane} {{Tags:["w"],Rotation:[-90f,0f]}}')
                ctx.wait(0.4)
                ctx.move_entity("@e[tag=w,limit=1]", (0.5, z + lane), (13.5, z + lane), y, 3.5)
                ctx.cmd("kill @e[tag=w]")
    ctx.cmd(f"gamemode spectator {p}")
    for mode in ("DETAILED", "PIXEL"):
        style(ctx, mode)
        for block, z in LANES:
            ctx.cmd(f"tp {p} 7 {Y + 3.4} {z + 1.7} 0 90")
            ctx.wait(1.0)
            ctx.shot(f"style-{mode.lower()}-{block}")
    # pixel decay stages on sand
    ctx.cmd(f"tp {p} 7 {Y + 3.4} {8 + 1.7} 0 90")
    for age in (0.7, 0.85, 0.95):
        hook(ctx, "prints.age", str(age))
        ctx.wait(1.0)
        ctx.shot(f"style-pixel-sand-age{int(age * 100)}")
