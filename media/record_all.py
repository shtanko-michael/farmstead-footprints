"""Full takes of every reel shot (60 fps frame sequences from the rig) -> media/out/takes/<shot>.mp4

    python -I media/record_all.py [shot ...]      default: every shot in film.SHOTS
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import film  # noqa: E402
import shoot  # noqa: E402

TAKES = os.path.join(film.OUT, "takes")
os.makedirs(TAKES, exist_ok=True)

for name in sys.argv[1:] or list(film.SHOTS):
    t = time.time()
    film.SHOTS[name](vet=False)
    n = shoot.encode("footprints_" + name, os.path.join(TAKES, name + ".mp4"), fps=60)
    print(f"{name}: {n} frames, {time.time() - t:.0f} s", flush=True)
