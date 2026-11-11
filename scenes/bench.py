"""Benchmark: a sand arena full of wandering mobs, measured with the mod on and off.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge bench

Every phase logs one line `bench <label>: fps=.. frameMs=.. renderUs=.. tickUs=.. alive=.. drawn=..` to the
client log (Bench.java). With another footprint mod copied into the dev client's mods folder, the "off" phase
measures that mod instead (ours draws nothing then).
"""
import re
import time

Y = 100
SIZE = 40
MOBS = [("cow", 40), ("sheep", 30), ("pig", 30), ("horse", 10), ("chicken", 30), ("villager", 10)]
FILL_SECONDS = 120
MEASURE_SECONDS = 20


def bench(ctx, label):
    hooks = ctx.client_dir / "testhooks"
    hooks.mkdir(parents=True, exist_ok=True)
    (hooks / f"{label}.bench").write_text(str(MEASURE_SECONDS), encoding="utf-8")
    ctx.wait(MEASURE_SECONDS + 3)


def set_enabled(ctx, on: bool):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*enabled = (true|false)", f"enabled = {'true' if on else 'false'}", text)
    cfg.write_text(text, encoding="utf-8")
    (ctx.client_dir / "testhooks" / "config.reload").write_text("", encoding="utf-8")
    ctx.wait(2)


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             "gamerule doMobSpawning false", f"gamemode spectator {p}", f"tp {p} {SIZE // 2} {Y + 8} {SIZE // 2}")
    ctx.wait(3)
    ctx.cmds(f"fill -2 {Y - 1} -2 {SIZE + 2} {Y - 1} {SIZE + 2} stone",
             f"fill -2 {Y} -2 {SIZE + 2} {Y + 8} {SIZE + 2} air",
             f"fill 0 {Y} 0 {SIZE} {Y} {SIZE} sand",
             f"fill -1 {Y + 1} -1 {SIZE + 1} {Y + 2} {SIZE + 1} oak_fence outline",
             "kill @e[type=!player]")
    ctx.wait(2)
    for kind, n in MOBS:
        for i in range(n):
            x, z = 2 + (i * 7 + len(kind) * 3) % (SIZE - 4), 2 + (i * 11 + len(kind) * 5) % (SIZE - 4)
            ctx.cmd(f"summon {kind} {x}.5 {Y + 1} {z}.5")
    set_enabled(ctx, True)
    # look down over the whole arena
    ctx.cmd(f"tp {p} {SIZE // 2} {Y + 26} {-6} 0 58")
    t = time.time()
    ctx.wait(FILL_SECONDS)
    ctx.shot("bench-arena")
    bench(ctx, "on")
    set_enabled(ctx, False)
    bench(ctx, "off")
    set_enabled(ctx, True)
    print(f"  bench done in {time.time() - t:.0f} s; results: grep 'bench ' in the client log")
