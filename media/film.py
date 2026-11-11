"""Imprint demo shoot on the capture rig (minecraft-api/game: capture-server + cinematic-mod client).

    python -I media/film.py scout          stills of every location -> media/out/scout-sheet.jpg
    python -I media/film.py test           one short take: the hero walks through snow, camera behind
    python -I media/film.py <shot>         record one shot of the reel (see SHOTS)

Setup (done once, see media/FILMING.md): capture-server runs world `imprint-world` (seed 2718281828,
Terralith, RCON 25892), the capture client has Imprint 1.21.1 in mods/ and preset "cinematic".
"""
import json
import math
import os
import sys
import time

RIG = r"C:\Users\admin\minecraft-api\game\cinematic-mod\tools\reels"
sys.path.insert(0, os.path.join(RIG, "s3"))
sys.path.insert(0, RIG)
sys.path.insert(0, r"C:\Users\admin\farmstead-mods\farmstead-mod-tools\test-matrix")
import shoot  # noqa: E402  (rig helpers: write/rec/stills/sheet/encode)
from rcon import Rcon  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)

_rcon = None


def rcon():
    global _rcon
    if _rcon is None:
        _rcon = Rcon(port=25892, password="imprint-film")
    return _rcon


def cmd(c):
    return rcon().cmd(c)


_heights = {}


def ground(x, z):
    """Y of the top solid surface at (x, z), asked from the server (spreadplayers on a marker)."""
    key = (round(x, 1), round(z, 1))
    if key in _heights:
        return _heights[key]
    if "marked" in cmd(f"forceload add {int(x) - 16} {int(z) - 16} {int(x) + 16} {int(z) + 16}").lower():
        time.sleep(2.5)  # freshly force-loaded chunks generate first
    raw = ""
    for _ in range(10):
        cmd("kill @e[tag=imprint_gh]")
        if "Summoned" in cmd(f'summon armor_stand {x} 300 {z} {{Tags:["imprint_gh"],Invisible:1b,NoGravity:1b,Marker:1b}}'):
            cmd(f"spreadplayers {x} {z} 0 1 false @e[tag=imprint_gh,limit=1]")
            raw = cmd("data get entity @e[tag=imprint_gh,limit=1] Pos")
            if "[" in raw:
                break
        time.sleep(1.5)
    cmd("kill @e[tag=imprint_gh]")
    try:
        y = float(raw.split("[", 1)[1].split(",")[1].strip().rstrip("d"))
    except (IndexError, ValueError):
        raise RuntimeError(f"no ground at {x},{z}: {raw!r}")
    _heights[key] = y
    return y


# name -> (x, z, look direction yaw for the overview, time of day)
LOCATIONS = {
    "snow": (1792, -1760, 0, 12500),
    "taiga": (1824, -1792, 90, 12500),
    "beach": (64, -320, 180, 6000),
    "mangrove": (-224, 224, 0, 6000),
    "savanna": (224, 192, 270, 7000),
    "snow-village": (1904, -2064, 0, 11500),
}


def scout():
    files, labels = [], []
    for name, (x, z, yaw, tod) in LOCATIONS.items():
        gy = ground(x, z)
        rad = math.radians(yaw)
        fx, fz = -math.sin(rad), math.cos(rad)
        pos = [x - fx * 10, gy + 6, z - fz * 10]
        look = [x + fx * 12, gy, z + fz * 12]
        got = shoot.stills("imprint_scout_" + name, [(pos, look)], time_of_day=tod, fov=70, settle=60)
        if got:
            files.append(got[-1])
            labels.append(f"{name} ({x},{int(gy)},{z})")
        print(name, gy, got[-1] if got else "NO FRAME", flush=True)
    out = shoot.sheet(files, os.path.join(OUT, "scout-sheet.jpg"), cols=3, w=640, labels=labels)
    print(out)


def walk_keys(start, end, frames, look_ahead=6.0):
    """Actor keys for a straight walk (y snaps to the ground in the rig)."""
    (x0, z0), (x1, z1) = start, end
    keys = []
    n = 6
    for i in range(n + 1):
        t = i / n
        x, z = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
        gy = ground(x, z)
        keys.append({"frame": int(frames * t), "pos": [x, gy, z],
                     "look": [x + (x1 - x0) / max(1e-6, math.hypot(x1 - x0, z1 - z0)) * look_ahead, gy + 1.5,
                              z + (z1 - z0) / max(1e-6, math.hypot(x1 - x0, z1 - z0)) * look_ahead],
                     "ease": "linear"})
    return keys


def test():
    frames = 330  # 5.5 s at 60 fps
    start, end = (1770.5, -1799.5), (1792.5, -1777.5)
    actor = {"name": "Imprint", "keys": walk_keys(start, end, frames)}
    gy = 92.0  # the cleared snowfield set: snow_block at 91 under a thin snow layer
    # camera: low and to the side, drifting along with the walker, looking slightly back at the trail
    cam = [
        {"frame": 0, "pos": [1766, gy + 2.6, -1788], "look": [1772, gy + 0.4, -1797], "fov": 62, "ease": "inOut"},
        {"frame": frames, "pos": [1786, gy + 3.2, -1768], "look": [1786, gy + 0.2, -1785], "fov": 62, "ease": "inOut"},
    ]
    name = shoot.write("imprint_test", cam, frames=frames, setup=["time set 12500"], actors=[actor], settle=60)
    res = shoot.rec(name)
    fs = shoot.frames_of(name)
    print(res, len(fs))
    if fs:
        picks = [fs[min(len(fs) - 1, int(len(fs) * f))] for f in (0.3, 0.55, 0.8, 0.99)]
        print(shoot.sheet(picks, os.path.join(OUT, "test-sheet.jpg"), cols=2, w=960))


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "scout"
    {"scout": scout, "test": test}[what]()
