"""Every footprint type on its own pad, one close-up shot per pad plus an overview.

Run: python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge gallery
"""
Y = 100
PAD_W, PAD_L, GAP = 5, 16, 3

# (name, surface, walker) — walker "player" / "player_barefoot" / an entity summon spec
PADS = [
    ("player-boots-snow", "snow_block", "player"),
    ("player-barefoot-sand", "sand", "player_barefoot"),
    ("husk-barefoot-snow", "snow_block", "husk"),
    ("villager-boots-sand", "sand", "villager"),
    ("baby-villager-snow", "snow_block", "villager{Age:-24000}"),
    ("wolf-paws-snow", "snow_block", "wolf"),
    ("cat-paws-sand", "sand", "cat"),
    ("horse-hooves-sand", "sand", "horse"),
    ("cow-hooves-snow", "snow_block", "cow"),
    ("pig-hooves-mud", "mud", "pig"),
    ("chicken-snow", "snow_block", "chicken"),
    ("iron-golem-sand", "sand", "iron_golem"),
    ("polar-bear-snow", "snow_block", "polar_bear"),
]


def pad_x(i):
    return i * (PAD_W + GAP)


def run(ctx):
    p = ctx.player
    end_x = pad_x(len(PADS)) + 2
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "gamerule doMobSpawning false",
             "time set noon", "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             f"gamemode creative {p}", f"tp {p} 2 {Y + 1} -4")
    ctx.wait(3)
    ctx.cmds(f"fill -4 {Y - 1} -6 {end_x} {Y - 1} {PAD_L + 4} stone",
             f"fill -4 {Y} -6 {end_x} {Y + 5} {PAD_L + 4} air",
             f"fill -4 {Y} -6 {end_x} {Y} {PAD_L + 4} smooth_stone", "kill @e[type=!player]")
    for i, (_, block, _) in enumerate(PADS):
        x = pad_x(i)
        ctx.cmd(f"fill {x} {Y} 0 {x + PAD_W - 1} {Y} {PAD_L - 1} {block}")
    ctx.wait(1.5)

    for i, (name, _, walker) in enumerate(PADS):
        cx = pad_x(i) + PAD_W / 2
        start, end = (cx, 1.0), (cx, PAD_L - 1.5)
        if walker.startswith("player"):
            boots = "air" if walker == "player_barefoot" else "iron_boots"
            ctx.cmd(f"item replace entity {p} armor.feet with {boots}")
            ctx.walk(start, end, Y + 1, speed=3.2)
        else:
            entity, _, nbt = walker.partition("{")
            nbt = "{" + nbt if nbt else ""
            tag = f"pad{i}"
            nbt = nbt[:-1] + f',Tags:["{tag}"]}}' if nbt else f'{{Tags:["{tag}"]}}'
            # stand next to the pad: the client only tracks entities near the camera
            ctx.cmd(f"tp {p} {cx + PAD_W} {Y + 1} {PAD_L / 2}")
            ctx.cmd(f"summon {entity} {start[0]} {Y + 1} {start[1]} {nbt}")
            ctx.wait(0.4)
            speed = 4.0 if entity in ("horse", "wolf") else 2.6
            ctx.move_entity(f"@e[tag={tag},limit=1]", start, end, Y + 1, speed)
            ctx.cmd(f"data merge entity @e[tag={tag},limit=1] {{NoAI:1b}}")  # stay at the end of the pad
    ctx.cmd(f"item replace entity {p} armor.feet with air")

    ctx.cmd(f"gamemode spectator {p}")
    for i, (name, _, _) in enumerate(PADS):
        cx = pad_x(i) + PAD_W / 2
        ctx.cmd(f"tp {p} {cx} {Y + 2.7} 0.2 0 50")
        ctx.wait(1.2)
        ctx.shot(f"{i + 1:02d}-{name}")
    ctx.cmd(f"tp {p} {pad_x(len(PADS)) / 2} {Y + 22} -10 0 55")
    ctx.wait(2)
    ctx.shot("00-overview")
