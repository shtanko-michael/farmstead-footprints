"""Short demo clips for the listing: frames recorded in game (TestHooks record), turned into GIFs by
media/gifs_from_frames.py.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge gifs
    python -I media/gifs_from_frames.py

Clips: crumble (prints wearing away on snow), styles (detailed <-> pixel), wake (a boat's foam), ripples (wading),
tryit (settings -> "Try it"), picker (searching blocks in the settings).
"""
import re
import threading
import time

Y = 100


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def record(ctx, name, seconds):
    hook(ctx, "record", f"{name} {seconds}")


def config(ctx, **values):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    for k, v in values.items():
        text = re.sub(rf"(?m)^#?\s*{k} = .*$", f"{k} = {v}", text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(1.2)


def walkers(ctx, z, block_y, who=("player", "horse", "wolf"), x0=0.5, x1=13.5):
    p = ctx.player
    for i, w in enumerate(who):
        lane = z + i * 1.3
        if w == "player":
            ctx.cmd(f"gamemode creative {p}")
            ctx.cmd(f"item replace entity {p} armor.feet with iron_boots")
            ctx.walk((x0, lane), (x1, lane), block_y, speed=3.2)
            ctx.cmd(f"item replace entity {p} armor.feet with air")
        else:
            ctx.cmd(f"tp {p} {(x0 + x1) / 2} {block_y} {lane - 3}")
            ctx.cmd(f'summon {w} {x0} {block_y} {lane} {{Tags:["w"],NoAI:1b}}')
            ctx.wait(0.4)
            ctx.move_entity("@e[tag=w,limit=1]", (x0, lane), (x1, lane), block_y, 3.5)
            ctx.cmd("kill @e[tag=w]")


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set 1000", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} 7 {Y + 1} -4")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 2} -6 34 {Y - 2} 40 stone", f"fill -6 {Y - 1} -6 34 {Y + 8} 40 air",
             f"fill -6 {Y - 1} -6 34 {Y - 1} 40 grass_block", "kill @e[type=!player]",
             f"fill 0 {Y - 1} 0 16 {Y - 1} 5 snow_block", f"fill 0 {Y - 1} 8 16 {Y - 1} 13 sand",
             f"fill 0 {Y - 2} 18 30 {Y - 2} 30 sand", f"fill 0 {Y - 1} 18 30 {Y - 1} 30 water")
    config(ctx, style='"DETAILED"', decay='"CRUMBLE"', lifetimeMultiplier="1.0")
    hook(ctx, "prints.clear")
    ctx.wait(1)

    # crumble: fresh prints on snow wearing away
    walkers(ctx, 1.0, Y)
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 8 {Y + 2.6} -1.2 0 52")
    ctx.wait(1.5)
    record(ctx, "crumble", 6)
    for k in range(60):
        hook(ctx, "prints.age", f"{0.42 + 0.58 * k / 59:.3f}")
        time.sleep(0.1)
    ctx.wait(0.5)

    # styles: the same prints, detailed and pixel
    hook(ctx, "prints.clear")
    walkers(ctx, 8.6, Y)
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 7 {Y + 2.2} 7.2 0 55")
    ctx.wait(2.5)
    record(ctx, "styles", 6)
    for s in ("DETAILED", "PIXEL", "DETAILED", "PIXEL"):
        hook(ctx, "style", s)
        time.sleep(1.5)
    hook(ctx, "style", "DETAILED")
    ctx.wait(0.5)

    # wake: a boat across the pool
    ctx.cmd(f"tp {p} 15 {Y + 6} 14 0 45")
    ctx.cmd(f'summon boat 1 {Y - 0.4} 24 {{Tags:["b"]}}')
    ctx.wait(1.0)
    record(ctx, "wake", 4.5)
    ctx.move_entity("@e[tag=b,limit=1]", (1, 24), (29, 24), Y - 0.4, 7.0)
    ctx.wait(0.8)
    ctx.cmd("kill @e[tag=b]")

    # ripples: wading through shallow water
    hook(ctx, "prints.clear")
    ctx.cmd(f"tp {p} 9 {Y + 3.5} 21 0 50")
    ctx.cmd(f'summon villager 2 {Y - 1} 27 {{Tags:["v"],NoAI:1b}}')
    ctx.wait(1.0)
    record(ctx, "ripples", 4.5)
    ctx.move_entity("@e[tag=v,limit=1]", (2, 27), (16, 27), Y - 1, 2.4)
    ctx.wait(0.8)
    ctx.cmd("kill @e[tag=v]")

    # tryit: the settings, "Try it", prints on the grass where you look
    hook(ctx, "prints.clear")
    ctx.cmd(f"gamemode creative {p}")
    ctx.cmd(f"tp {p} 8 {Y} 0.5 0 45")
    ctx.wait(1.0)
    hook(ctx, "config.open")
    ctx.wait(1.0)
    hook(ctx, "config.tab", "footprints")
    ctx.wait(0.8)
    record(ctx, "tryit", 4.5)
    ctx.wait(1.6)
    hook(ctx, "try.it")
    ctx.wait(3.2)

    # picker: searching blocks
    hook(ctx, "config.open")
    ctx.wait(1.0)
    hook(ctx, "config.tab", "surfaces")
    ctx.wait(0.6)
    record(ctx, "picker", 4)
    for q in ("", "s", "sa", "san", "sand", "snow"):
        hook(ctx, "config.pick", f"surfaces.include {q}")
        time.sleep(0.6)
    ctx.wait(1.0)
    hook(ctx, "try.it")
    ctx.wait(1)
