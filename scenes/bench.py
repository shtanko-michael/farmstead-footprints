"""Benchmark: a sand arena full of mobs on the move, measured as frame rate.

    BENCH_MODE=ours python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge bench
    BENCH_MODE=none ...    (our mod switched off: the game alone)
    BENCH_MODE=<label> ... (our mod off, another footprint mod in the dev client's mods folder)

media/compare.py runs all the modes in a row. The scene logs `bench <label>: fps=.. frameMs=.. renderUs=..
tickUs=.. alive=.. drawn=..` to the client log (Bench.java); fps and frameMs are what the player gets, the rest is
our own cost and is 0 when our mod is off. For a fair comparison every mod keeps prints for about a minute on sand.
"""
import os
import re
import time

Y = 100
SIZE = 40
MOBS = [("cow", 80), ("sheep", 60), ("pig", 60), ("horse", 20), ("chicken", 60), ("villager", 20)]
FILL_SECONDS = 120
MEASURE_SECONDS = 20


def bench(ctx, label):
    hooks = ctx.client_dir / "testhooks"
    hooks.mkdir(parents=True, exist_ok=True)
    (hooks / f"{label}.bench").write_text(str(MEASURE_SECONDS), encoding="utf-8")
    ctx.wait(MEASURE_SECONDS + 3)


def ours(ctx, on: bool):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*enabled = (true|false)", f"enabled = {'true' if on else 'false'}", text)
    # sand keeps prints 180 s by default; a third of that = one minute, like the others are set to
    text = re.sub(r"(?m)^#?\s*lifetimeMultiplier = .*$", "lifetimeMultiplier = 0.33", text)
    cfg.write_text(text, encoding="utf-8")
    (ctx.client_dir / "testhooks" / "config.reload").write_text("", encoding="utf-8")
    ctx.wait(2)


def run(ctx):
    mode = os.environ.get("BENCH_MODE", "ours")
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             "gamerule doMobSpawning false", f"gamemode spectator {p}", f"tp {p} {SIZE // 2} {Y + 8} {SIZE // 2}")
    ctx.wait(3)
    ctx.cmds(f"fill -6 {Y - 1} -6 {SIZE + 6} {Y - 1} {SIZE + 6} stone",
             f"fill -6 {Y} -6 {SIZE + 6} {Y + 12} {SIZE + 6} air",
             f"fill 0 {Y} 0 {SIZE} {Y} {SIZE} sand",
             f"fill -1 {Y + 1} -1 {SIZE + 1} {Y + 2} {SIZE + 1} oak_fence outline",
             "kill @e[type=!player]")
    ctx.wait(2)
    for kind, n in MOBS:
        for i in range(n):
            x, z = 2 + (i * 7 + len(kind) * 3) % (SIZE - 4), 2 + (i * 11 + len(kind) * 5) % (SIZE - 4)
            ctx.cmd(f"summon {kind} {x}.5 {Y + 1} {z}.5")
    # quick, restless mobs: more steps, more prints
    ctx.cmd("effect give @e[type=!player] speed 600 2 true")
    ours(ctx, mode == "ours")
    ctx.cmd(f"tp {p} {SIZE // 2} {Y + 26} {-6} 0 58")
    t = time.time()
    ctx.wait(FILL_SECONDS)
    ctx.shot(f"bench-{mode}")
    bench(ctx, mode)
    print(f"  bench {mode} done in {time.time() - t:.0f} s")
