"""Frame-rate comparison with other footprint mods, same scene, one mod at a time (NeoForge 1.21.1 dev client).

    python -I media/compare.py <dir with the other mods' jars> [bench|look]

Modes: none (no footprints at all), ours, then one per jar in the folder (our mod switched off). Each competitor
gets a one-minute print lifetime where it has a setting for it, like ours. Prints a table from the client logs.
"""
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
TARGET = "1.21.1-neoforge"
CLIENT = MOD / "run" / TARGET / "client"
SCENE = MOD.parent / "farmstead-mod-tools" / "test-matrix" / "scene.py"
OUT = MOD / "build" / "scenes" / TARGET


def prepare(jar: Path | None):
    mods = CLIENT / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    for f in mods.glob("*.jar"):
        f.unlink()
    if jar is not None:
        shutil.copy2(jar, mods / jar.name)
    # FootprintParticle: prints last 5 s by default; one minute like the others
    (CLIENT / "config").mkdir(parents=True, exist_ok=True)
    (CLIENT / "config" / "footprintparticle.json").write_text(json.dumps({"printLifetime": 60.0}), encoding="utf-8")


def run(mode: str, scene: str = "bench") -> str:
    env = dict(os.environ, BENCH_MODE=mode)
    subprocess.run([sys.executable, "-I", str(SCENE), str(MOD), TARGET, scene], env=env, check=False)
    if scene != "bench":
        return f"{scene} {mode}: screenshots in {OUT}"
    log = (OUT / "client.log").read_text(encoding="utf-8", errors="replace")
    lines = [l for l in log.splitlines() if f"bench {mode}: gameFps=" in l]
    return lines[-1].split("Footprints/]: ")[-1] if lines else f"bench {mode}: no result"


def main():
    jars = sorted(Path(sys.argv[1]).glob("*.jar")) if len(sys.argv) > 1 else []
    scene = sys.argv[2] if len(sys.argv) > 2 else "bench"
    results = []
    prepare(None)
    if scene == "bench":
        results.append(run("none"))
    results.append(run("ours", scene))
    for jar in jars:
        prepare(jar)
        label = re.sub(r"[^a-z]+", "", jar.stem.split("-")[0].lower())
        results.append(f"{run(label, scene)}   [{jar.name}]")
    prepare(None)
    print("\n".join(results))


if __name__ == "__main__":
    main()
