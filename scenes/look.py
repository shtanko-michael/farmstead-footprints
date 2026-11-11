"""Side-by-side look: the same walkers on the same lanes, photographed from the same spots, one footprint mod at a
time (media/compare.py runs it per mod). Lanes: snow block, snow layer, sand, mud, grass, gravel, each
a fenced pen of cows, a horse, wolves, a chicken, a villager and a pig walking on their own for a minute.

    BENCH_MODE=ours|<label> python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge look
"""
import os
import re

Y = 100
LANES = [("snow_block", 0), ("snow", 5), ("sand", 10), ("mud", 15), ("grass_block", 20), ("gravel", 25)]


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def ours(ctx, on):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*enabled = (true|false)", f"enabled = {'true' if on else 'false'}", text)
    text = re.sub(r"(?m)^#?\s*lifetimeMultiplier = .*$", "lifetimeMultiplier = 1.0", text)
    text = re.sub(r"(?m)^#?\s*profiles = .*$", "profiles = []", text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(2)


def run(ctx):
    mode = os.environ.get("BENCH_MODE", "ours")
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             "gamerule doMobSpawning false", "kill @e[type=!player]", f"gamemode spectator {p}", f"tp {p} 20 {Y + 8} 14")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 2} -6 46 {Y} 36 stone", f"fill -6 {Y + 1} -6 46 {Y + 12} 36 air", "kill @e[type=item]")
    for block, z in LANES:
        if block == "snow":
            ctx.cmds(f"fill 0 {Y} {z} 40 {Y} {z + 3} dirt", f"fill 0 {Y + 1} {z} 40 {Y + 1} {z + 3} snow[layers=2]")
        else:
            ctx.cmd(f"fill 0 {Y} {z} 40 {Y} {z + 3} {block}")
    ours(ctx, mode == "ours")
    ctx.wait(1)
    # every lane is a fenced pen of mobs walking on their own (other mods ignore teleported mobs): all lanes at
    # once, so every mod gets the same minute of walking before the photos
    for block, z in LANES:
        y = Y + 1 + (0.25 if block == "snow" else 0)
        b = Y + 1 + (1 if block == "snow" else 0)
        ctx.cmds(f"fill -1 {b} {z - 1} 41 {b + 1} {z - 1} oak_fence", f"fill -1 {b} {z + 4} 41 {b + 1} {z + 4} oak_fence",
                 f"fill -1 {b} {z - 1} -1 {b + 1} {z + 4} oak_fence", f"fill 41 {b} {z - 1} 41 {b + 1} {z + 4} oak_fence")
        for i, kind in enumerate(["cow", "cow", "horse", "wolf", "wolf", "chicken", "villager", "pig"]):
            ctx.cmd(f"summon {kind} {3 + i * 4}.5 {y} {z + 1.5}")
    ctx.cmds("effect give @e[type=!player] speed 300 1 true", "kill @e[type=item]")
    ctx.wait(90)
    ctx.cmd(f"tp {p} 17 {Y + 22} -10 0 55")
    ctx.wait(3)
    ctx.shot(f"look-{mode}-overview")
    # straight down from five blocks: the ground of one lane fills the frame
    for block, z in LANES:
        ctx.cmd(f"tp {p} 14 {Y + 5.5} {z + 2} 0 90")
        ctx.wait(2)
        ctx.shot(f"look-{mode}-{block}")
