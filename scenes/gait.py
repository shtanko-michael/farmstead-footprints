"""Four-legged gait check: each animal walks a long straight sand pad, photographed straight down.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge gait

A walking quadruped leaves a single alternating line of prints per side: the hind foot lands in (or next to) the
front print of the same side, left and right sides take turns. Two prints side by side on one side, or a
left-left-right-right rhythm, is the bug this scene exists to catch.
"""
Y = 100
PAD_W, PAD_L, GAP = 5, 26, 2
PADS = [("horse", 4.0), ("horse", 7.0), ("donkey", 3.5), ("cow", 2.6), ("polar_bear", 2.6), ("wolf", 4.0)]


def run(ctx):
    p = ctx.player
    end_x = len(PADS) * (PAD_W + GAP) + 2
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} 2 {Y + 1} -4")
    ctx.wait(3)
    ctx.cmds(f"fill -4 {Y - 1} -6 {end_x} {Y - 1} {PAD_L + 4} stone",
             f"fill -4 {Y} -6 {end_x} {Y + 5} {PAD_L + 4} air",
             f"fill -4 {Y} -6 {end_x} {Y} {PAD_L + 4} smooth_stone", "kill @e[type=!player]")
    for i in range(len(PADS)):
        x = i * (PAD_W + GAP)
        ctx.cmd(f"fill {x} {Y} 0 {x + PAD_W - 1} {Y} {PAD_L - 1} sand")
    ctx.wait(1.5)
    for i, (entity, speed) in enumerate(PADS):
        cx = i * (PAD_W + GAP) + PAD_W / 2
        tag = f"g{i}"
        ctx.cmd(f"tp {p} {cx + PAD_W} {Y + 1} {PAD_L / 2}")
        ctx.cmd(f'summon {entity} {cx} {Y + 1} 1.0 {{Tags:["{tag}"]}}')
        ctx.wait(0.4)
        ctx.move_entity(f"@e[tag={tag},limit=1]", (cx, 1.0), (cx, PAD_L - 1.5), Y + 1, speed)
        ctx.cmd(f"kill @e[tag={tag}]")
    ctx.cmd(f"gamemode spectator {p}")
    for i, (entity, speed) in enumerate(PADS):
        cx = i * (PAD_W + GAP) + PAD_W / 2
        for half, z in (("a", 7.5), ("b", 18.5)):
            ctx.cmd(f"tp {p} {cx} {Y + 7} {z} 0 90")
            ctx.wait(1.2)
            ctx.shot(f"gait-{i}-{entity}-{speed:g}-{half}")
