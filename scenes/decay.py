"""Crumbling decay: the same prints photographed at several ages (TestHooks prints.age jumps every print to a share
of its life), straight down, on snow, sand and mud.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge decay
"""
Y = 100
LANES = [("snow_block", 0, "player"), ("sand", 4, "horse"), ("mud", 8, "wolf")]
AGES = [0.0, 0.6, 0.72, 0.82, 0.9, 0.95]


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} -2 {Y + 1} 0")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 1} -4 20 {Y - 1} 16 stone", f"fill -6 {Y} -4 20 {Y + 5} 16 air",
             f"fill -6 {Y} -4 20 {Y} 16 smooth_stone", "kill @e[type=!player]")
    for block, z, _ in LANES:
        ctx.cmd(f"fill 0 {Y} {z} 14 {Y} {z + 2} {block}")
    hook(ctx, "prints.clear")
    ctx.wait(1.5)
    for block, z, who in LANES:
        if who == "player":
            ctx.cmd(f"item replace entity {p} armor.feet with iron_boots")
            ctx.walk((0.5, z + 1.5), (13.5, z + 1.5), Y + 1, speed=3.2)
            ctx.cmd(f"item replace entity {p} armor.feet with air")
        else:
            ctx.cmd(f"tp {p} 7 {Y + 1} {z - 2}")
            ctx.cmd(f'summon {who} 0.5 {Y + 1} {z + 1.5} {{Tags:["w"],Rotation:[-90f,0f]}}')
            ctx.wait(0.4)
            ctx.move_entity("@e[tag=w,limit=1]", (0.5, z + 1.5), (13.5, z + 1.5), Y + 1, 3.5)
            ctx.cmd("kill @e[tag=w]")
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 7 {Y + 5.2} 5.5 0 90")
    ctx.wait(1.5)
    for age in AGES:
        hook(ctx, "prints.age", str(age))
        ctx.wait(1.0)
        ctx.shot(f"decay-{int(age * 100):02d}")
