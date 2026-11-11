"""Water and rails: a boat's wake, wading rings, droplets from wet feet onto stone, sparks under a fast cart.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge water
"""
Y = 100


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} -3 {Y + 1} 5")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 2} -6 40 {Y - 2} 24 stone", f"fill -6 {Y - 1} -6 40 {Y + 6} 24 air",
             f"fill -6 {Y - 1} -6 40 {Y - 1} 24 smooth_stone", "kill @e[type=!player]",
             # a one-deep pool: boats float, walkers wade
             f"fill 0 {Y - 1} 0 30 {Y - 1} 10 sand", f"fill 0 {Y} 0 30 {Y} 10 water",
             f"fill -6 {Y} -6 40 {Y} -1 smooth_stone", f"fill -6 {Y} 11 40 {Y} 24 smooth_stone",
             f"fill -6 {Y} 0 -1 {Y} 10 smooth_stone", f"fill 31 {Y} 0 40 {Y} 10 smooth_stone",
             # rails beside it
             f"fill 0 {Y + 1} 16 30 {Y + 1} 16 rail", f"fill 12 {Y + 1} 16 18 {Y + 1} 16 powered_rail[powered=true]",
             f"fill 12 {Y} 16 18 {Y} 16 redstone_block")
    ctx.wait(2)
    ctx.cmd(f"tp {p} 15 {Y + 3} -3")
    # boats split by wood in 1.21.2
    mc = ctx.out_dir.name.split("-")[0]
    parts = [int(x) for x in mc.split(".")]
    boat = "oak_boat" if parts[0] >= 26 or parts[:2] == [1, 21] and len(parts) > 2 and parts[2] >= 2 else "boat"
    ctx.cmd(f'summon {boat} 1 {Y + 0.6} 3 {{Tags:["b"]}}')
    ctx.cmd(f'summon villager 1 {Y} 8 {{Tags:["v"],NoAI:1b}}')
    ctx.cmd(f'summon minecart 1 {Y + 1} 16 {{Tags:["m"]}}')
    ctx.wait(0.5)
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 15 {Y + 9} -5 0 52")
    ctx.move_entity("@e[tag=v,limit=1]", (1, 8), (16, 8), Y, 2.5)
    ctx.move_entity("@e[tag=b,limit=1]", (1, 3), (29, 3), Y + 0.6, 7.0)
    ctx.shot("water-wake")
    ctx.cmd(f"tp {p} 15 {Y + 3.5} 5.5 0 90")
    ctx.wait(0.8)
    ctx.shot("water-top")
    # sparks: watch the cart race past the powered stretch
    ctx.cmd(f"tp {p} 15 {Y + 3} 13 0 40")
    ctx.wait(0.5)
    # move in the background and photograph mid-run: sparks live a fraction of a second
    import threading
    mover = threading.Thread(target=ctx.move_entity, args=("@e[tag=m,limit=1]", (1, 16), (29, 16), Y + 1, 9.0))
    mover.start()
    ctx.wait(1.45)
    ctx.shot("water-sparks")
    mover.join()
