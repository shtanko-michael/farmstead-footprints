"""Quick check of one target: prints on sand (detailed and pixel), a boat's wake on water.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.4-fabric quick
"""
import re

Y = 100


def hook(ctx, name, text=""):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text(text, encoding="utf-8")


def style(ctx, value):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    if not cfg.exists():
        return
    text = cfg.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^#?\s*style = .*$", f'style = "{value}"', text)
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    ctx.wait(1.2)


def run(ctx):
    p = ctx.player
    mc = ctx.out_dir.name.split("-")[0]
    parts = [int(x) for x in mc.split(".")]
    boat = "oak_boat" if parts[0] >= 26 or parts[:2] == [1, 21] and len(parts) > 2 and parts[2] >= 2 else "boat"
    ctx.cmds("gamerule doMobSpawning false", "time set noon", "gamerule doDaylightCycle false", "weather clear",
             f"gamemode creative {p}", f"tp {p} 7 {Y + 1} -3")
    ctx.wait(3)
    ctx.cmds(f"fill -4 {Y - 2} -4 30 {Y - 2} 20 stone", f"fill -4 {Y - 1} -4 30 {Y + 5} 20 air",
             f"fill -4 {Y - 1} -4 30 {Y - 1} 20 smooth_stone", f"fill 0 {Y - 1} 0 14 {Y - 1} 3 sand",
             f"fill 0 {Y - 2} 8 26 {Y - 2} 16 sand", f"fill 0 {Y - 1} 8 26 {Y - 1} 16 water", "kill @e[type=!player]")
    style(ctx, "DETAILED")
    hook(ctx, "prints.clear")
    ctx.wait(1)
    ctx.cmd(f"item replace entity {p} armor.feet with iron_boots")
    ctx.walk((0.5, 1), (13.5, 1), Y, speed=3.2)
    ctx.cmd(f'summon horse 0.5 {Y} 2.5 {{Tags:["w"],NoAI:1b}}')
    ctx.wait(0.4)
    ctx.move_entity("@e[tag=w,limit=1]", (0.5, 2.5), (13.5, 2.5), Y, 3.5)
    ctx.cmd(f"gamemode spectator {p}")
    ctx.cmd(f"tp {p} 7 {Y + 3} 1.7 0 90")
    ctx.wait(1.2)
    ctx.shot("quick-detailed")
    style(ctx, "PIXEL")
    ctx.shot("quick-pixel")
    style(ctx, "DETAILED")
    ctx.cmd(f"tp {p} 13 {Y + 7} 3 0 50")
    ctx.cmd(f'summon {boat} 1 {Y - 0.4} 12 {{Tags:["b"]}}')
    ctx.wait(0.5)
    ctx.move_entity("@e[tag=b,limit=1]", (1, 12), (25, 12), Y - 0.4, 7.0)
    ctx.shot("quick-wake")
