"""Wet prints: a villager and a wolf wade out of a pool onto stone and planks and leave damp prints that fade
as their feet dry.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge wet
"""
Y = 100


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doMobSpawning false",
             "kill @e[type=!player]", f"gamemode spectator {p}", f"tp {p} 12 {Y + 8} 4")
    ctx.wait(3)
    ctx.cmds(f"fill -4 {Y - 2} -4 30 {Y} 12 stone", f"fill -4 {Y + 1} -4 30 {Y + 8} 12 air",
             f"fill 0 {Y} 6 30 {Y} 9 oak_planks",
             # a one-deep pool at the start of both lanes
             f"fill 0 {Y} 0 3 {Y} 9 water")
    ctx.wait(1)
    ctx.cmds(f"summon villager 1.5 {Y} 1.5 {{NoAI:1b}}", f"summon wolf 1.5 {Y} 7.5 {{NoAI:1b}}")
    ctx.wait(3)  # soak
    ctx.move_entity("@e[type=villager,limit=1]", (1.5, 1.5), (26.5, 1.5), Y + 1, 2.5)
    ctx.move_entity("@e[type=wolf,limit=1]", (1.5, 7.5), (26.5, 7.5), Y + 1, 2.5)
    ctx.cmd(f"tp {p} 14 {Y + 12} -6 0 55")
    ctx.wait(2)
    ctx.shot("wet-overview")
    ctx.cmd(f"tp {p} 6 {Y + 3.5} 4.5 -90 55")
    ctx.wait(2)
    ctx.shot("wet-close")
