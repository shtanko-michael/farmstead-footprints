"""Settings: the generated file, a broken value reported on reload, the settings screen.

Run: python -I ../farmstead-mod-tools/test-matrix/scene.py . 1.21.1-neoforge config
"""
import time
from pathlib import Path


def hook(ctx, name):
    d = ctx.client_dir / "testhooks"
    d.mkdir(parents=True, exist_ok=True)
    (d / name).write_text("", encoding="utf-8")
    time.sleep(1.5)


def run(ctx):
    cfg = ctx.client_dir / "config" / "farmstead_footprints-client.toml"
    print("  config file exists:", cfg.exists())
    text = cfg.read_text(encoding="utf-8")
    (ctx.out_dir / "farmstead_footprints-client.toml").write_text(text, encoding="utf-8")
    # a broken value and a preset switch, then reload through the hook
    text = text.replace('preset = "default"', 'preset = "performance"').replace("#opacity = 0.95", "opacity = 7")
    cfg.write_text(text, encoding="utf-8")
    hook(ctx, "config.reload")
    (ctx.out_dir / "footprints-client.after.toml").write_text(cfg.read_text(encoding="utf-8"), encoding="utf-8")
    hook(ctx, "config.open")
    ctx.wait(1)
    ctx.shot("config-screen")
