"""Settings screen v2: every tab, the block picker with a search, and "Try it" on the ground.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge ui
"""
Y = 100


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule doMobSpawning false", "time set noon", "gamerule doDaylightCycle false", "weather clear",
             f"gamemode creative {p}", f"tp {p} 0 {Y + 1} 0 0 35")
    ctx.wait(3)
    ctx.cmds(f"fill -8 {Y - 1} -4 8 {Y - 1} 12 stone", f"fill -8 {Y} -4 8 {Y + 5} 12 air",
             f"fill -8 {Y} -4 8 {Y} 12 sand", "kill @e[type=!player]")
    ctx.wait(1.5)
    hook(ctx, "config.open")
    ctx.wait(1.5)
    for tab in ("general", "footprints", "effects", "surfaces", "about"):
        hook(ctx, "config.tab", tab)
        ctx.wait(0.8)
        ctx.shot(f"ui-{tab}")
    hook(ctx, "config.tab", "surfaces")
    ctx.wait(0.5)
    hook(ctx, "config.pick", "surfaces.include sand")
    ctx.wait(1.0)
    ctx.shot("ui-picker")
    hook(ctx, "try.it")
    ctx.wait(1.5)
    ctx.shot("ui-try")
