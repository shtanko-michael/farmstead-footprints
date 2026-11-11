"""Render benchmark under a full pool: 8192 prints around the player, viewed from above and from eye level.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge stress

Phases (each logs `bench <label>: ...` to the client log, see Bench.java):
  on-top / on-eye   the mod drawing 8192 prints, camera looking down / looking along the ground
  off-top / off-eye   the mod switched off (what the game costs without it)
"""
import re

Y = 100
MEASURE = 15


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def bench(ctx, label):
    hook(ctx, f"{label}.bench", str(MEASURE))
    ctx.wait(MEASURE + 3)


def config(ctx, **values):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    text = cfg.read_text(encoding="utf-8")
    for key, value in values.items():
        text = re.sub(rf"(?m)^#?\s*{key} = .*$", f"{key} = {value}", text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(2)


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule logAdminCommands false", "gamerule sendCommandFeedback false", "time set noon",
             "gamerule doDaylightCycle false", "weather clear", "gamerule doWeatherCycle false",
             "gamerule doMobSpawning false", "kill @e[type=!player]", f"gamemode spectator {p}",
             f"tp {p} 0 {Y + 30} 0")
    ctx.wait(3)
    ctx.cmds(f"fill -45 {Y - 1} -45 45 {Y - 1} 45 stone", f"fill -45 {Y} -45 45 {Y} 45 sand",
             f"fill -45 {Y + 1} -45 45 {Y + 20} 45 air")
    ctx.wait(2)
    config(ctx, enabled="true", maxPrints="8192", renderDistance="64")
    ctx.cmd(f"tp {p} 0 {Y + 30} 0")
    ctx.wait(1)
    hook(ctx, "stress.fill")
    ctx.wait(2)

    views = {"top": f"tp {p} 0 {Y + 30} -30 0 50", "eye": f"tp {p} 0 {Y + 2.6} -40 0 8"}
    for view, tp in views.items():
        ctx.cmd(tp)
        ctx.wait(3)
        ctx.shot(f"stress-{view}")
        bench(ctx, f"on-{view}")
        config(ctx, enabled="false")
        bench(ctx, f"off-{view}")
        config(ctx, enabled="true")
