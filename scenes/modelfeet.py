"""Model reader check: summon vanilla mobs, log what ModelFeet reads from each model next to the hand-made profile.

    python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge modelfeet
"""
Y = 100
MOBS = ["cow", "pig", "sheep", "wolf", "cat", "fox", "horse", "llama", "polar_bear", "spider", "chicken", "villager",
        "zombie", "creeper", "iron_golem", "goat", "turtle", "camel", "sniffer", "frog"]


def run(ctx):
    p = ctx.player
    ctx.cmds("gamerule doMobSpawning false", "time set noon", f"gamemode creative {p}", f"tp {p} 0 {Y + 1} -4")
    ctx.wait(3)
    ctx.cmds(f"fill -20 {Y - 1} -6 20 {Y - 1} 6 stone", f"fill -20 {Y} -6 20 {Y + 6} 6 air", "kill @e[type=!player]")
    for i, m in enumerate(MOBS):
        ctx.cmd(f"summon {m} {-19 + i * 2} {Y} 2 {{NoAI:1b}}")
    ctx.wait(2)
    hooks = ctx.client_dir / "testhooks"
    hooks.mkdir(parents=True, exist_ok=True)
    (hooks / "model.feet").write_text("", encoding="utf-8")
    ctx.wait(2)
    log = (ctx.client_dir / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace")
    for line in log.splitlines():
        if "model.feet" in line:
            print("  " + line.split("]: ", 1)[-1])
