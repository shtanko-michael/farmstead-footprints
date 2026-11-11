"""Hopping gait check: a rabbit and a frog cross the sand; every bound should leave the two hind feet side by
side ahead of the two front feet.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge hop
"""
Y = 100


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doMobSpawning false",
             "kill @e[type=!player]", f"gamemode spectator {p}", f"tp {p} 8 {Y + 9} 3")
    ctx.wait(3)
    ctx.cmds(f"fill -2 {Y - 1} -4 20 {Y - 1} 10 stone", f"fill -2 {Y + 1} -4 20 {Y + 8} 10 air",
             f"fill 0 {Y} 0 18 {Y} 6 sand")
    # a rabbit and a frog crossing the sand (moved by the scene; bounds are measured by distance)
    ctx.cmds(f"summon rabbit 1.5 {Y + 1} 1.5 {{NoAI:1b}}", f"summon frog 1.5 {Y + 1} 4.5 {{NoAI:1b}}")
    ctx.wait(1)
    ctx.move_entity("@e[type=rabbit,limit=1]", (1.5, 1.5), (16.5, 1.5), Y + 1, 4)
    ctx.move_entity("@e[type=frog,limit=1]", (1.5, 4.5), (16.5, 4.5), Y + 1, 3)
    ctx.cmd(f"tp {p} 9 {Y + 7} 3 0 90")
    ctx.wait(2)
    ctx.shot("hop-top")
