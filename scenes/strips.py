"""Five surface strips; the player and four kinds of mobs walk them; overview + close-up shots.

Run: python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge strips
"""
Y = 100          # floor level; entities stand at Y + 1
LANES = [        # (block, z of the lane's first row)
    ("sand", 0), ("snow_block", 5), ("mud", 10), ("gravel", 15), ("grass_block", 20),
]


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode survival {p}", f"tp {p} 20 {Y + 1} -3")
    ctx.wait(3)
    ctx.cmds(f"fill -2 {Y - 1} -6 44 {Y - 1} 26 stone", f"fill -2 {Y + 1} -6 44 {Y + 6} 26 air")
    for block, z in LANES:
        ctx.cmd(f"fill 0 {Y} {z} 40 {Y} {z + 3} {block}")
    ctx.wait(1.5)

    # the player walks the sand lane and comes back along the snow lane
    ctx.walk((1.5, 1.5), (38.5, 1.5), Y + 1)
    ctx.walk((38.5, 6.5), (1.5, 6.5), Y + 1)

    # mobs: wolf on mud, horse on gravel, villager and chicken on grass (control: no prints) and sand
    ctx.cmds("kill @e[type=!player]",
             f"summon wolf 1.5 {Y + 1} 11.5", f"summon horse 1.5 {Y + 1} 17.5",
             f"summon villager 1.5 {Y + 1} 21.5", f"summon chicken 1.5 {Y + 1} 3")
    ctx.wait(1)
    ctx.move_entity("@e[type=wolf,limit=1]", (1.5, 11.5), (38.5, 11.5), Y + 1, 5)
    ctx.move_entity("@e[type=horse,limit=1]", (1.5, 17.5), (38.5, 17.5), Y + 1, 6)
    ctx.move_entity("@e[type=villager,limit=1]", (1.5, 21.5), (38.5, 21.5), Y + 1, 3)
    ctx.move_entity("@e[type=chicken,limit=1]", (1.5, 3), (30, 3), Y + 1, 2)

    # overview from above the start of the lanes, looking along +X
    ctx.cmds(f"gamemode spectator {p}", f"tp {p} -6 {Y + 9} 11 -90 38")
    ctx.wait(2)
    ctx.shot("overview")
    # close-up of the sand and snow lanes
    ctx.cmd(f"tp {p} 12 {Y + 3.2} -1.5 -60 55")
    ctx.wait(1.5)
    ctx.shot("closeup-sand-snow")
    ctx.cmd(f"tp {p} 14 {Y + 3.2} 9 -60 55")
    ctx.wait(1.5)
    ctx.shot("closeup-mud-gravel")
