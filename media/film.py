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


def ring(x, z, tod=6000, height=2.0, label="ring"):
    """Eight stills from (x, z) looking outward every 45 degrees: which way is the beautiful background."""
    gy = ground(x, z)
    shots = []
    for i in range(8):
        a = math.radians(i * 45)
        shots.append(([x, gy + height, z], [x - math.sin(a) * 20, gy + 1, z + math.cos(a) * 20]))
    got = shoot.stills(f"imprint_{label}", shots, time_of_day=tod, fov=70, settle=60)
    names = ["S", "SW", "W", "NW", "N", "NE", "E", "SE"]
    out = shoot.sheet(got, os.path.join(OUT, f"{label}-sheet.jpg"), cols=4, w=480,
                      labels=[f"{label} {n} ({x},{gy:.0f},{z})" for n in names[:len(got)]])
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


HOOKS = r"C:\Users\admin\minecraft-api\game\capture-client\testhooks"


def clear_prints():
    """Wipes prints left by earlier takes (Imprint test hook)."""
    os.makedirs(HOOKS, exist_ok=True)
    open(os.path.join(HOOKS, "prints.clear"), "w").close()
    time.sleep(1.0)


def mob_cues(selector, points, frames, y, every=2, start_frame=0):
    """Teleport a NoAI mob along a polyline over `frames`, one tp every `every` frames, facing its way."""
    segs = [(points[i], points[i + 1]) for i in range(len(points) - 1)]
    lengths = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in segs]
    total = sum(lengths)
    cues = []
    for f in range(0, frames + 1, every):
        d = total * f / frames
        for (a, b), L in zip(segs, lengths):
            if d <= L or (a, b) == segs[-1]:
                t = 0 if L == 0 else min(1, d / L)
                x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
                yaw = math.degrees(math.atan2(-(b[0] - a[0]), b[1] - a[1]))
                break
            d -= L
        cues.append({"frame": start_frame + f, "run": f"tp {selector} {x:.3f} {y:.3f} {z:.3f} {yaw:.1f} 0"})
    return cues


def summon(kind, tag, at, y, extra="", toward=None):
    if toward is not None:  # spawn already facing the way it will walk
        yaw = math.degrees(math.atan2(-(toward[0] - at[0]), toward[1] - at[1]))
        extra += ",Rotation:[%.1ff,0f]" % yaw
    nbt = '{Tags:["imprint_film","%s"],NoAI:1b,Silent:1b,PersistenceRequired:1b%s}' % (tag, extra)
    return f"summon {kind} {at[0]} {y} {at[1]} {nbt}"


EYE = 1.62  # the rig puts the camera player's FEET at "pos", so the lens sits this much higher


def vet_or_record(name, cam, frames, setup, actors=None, cues=None, vet=True, tod=12500, feet=False):
    """vet: every 30th frame only, a sheet of 6 frames to judge the framing; record: the full take.

    Camera keys are lens coordinates: "pos" is where the lens is and it aims exactly at "look". The rig itself
    takes "pos" as the feet and aims from there, so both are shifted down by EYE before writing. feet=True passes
    the keys through raw (the first shots were framed that way, by eye, and look right as they are)."""
    if not feet:
        cam = [dict(k, pos=[k["pos"][0], k["pos"][1] - EYE, k["pos"][2]],
                    look=[k["look"][0], k["look"][1] - EYE, k["look"][2]]) for k in cam]
    clear_prints()
    cues = sorted(cues or [], key=lambda c: c["frame"])  # the rig fires cues in list order
    stride = 30 if vet else 1
    # earlier actors drop into the void instead of dying on screen (a red flash and a puff of smoke)
    cmd("tp @e[tag=imprint_film] ~ -300 ~")
    cmd("kill @e[tag=imprint_film]")
    cmd("time set %d" % tod)
    time.sleep(1.0)
    scene = shoot.write(name, cam, frames=frames, setup=["time set %d" % tod, "kill @e[tag=imprint_film]"] + setup,
                        actors=actors or [], cues=cues or [], settle=150, stride=stride)
    res = shoot.rec(scene)
    fs = shoot.frames_of(scene)
    print(res)
    if fs:
        picks = [fs[min(len(fs) - 1, int(len(fs) * f))] for f in (0.0, 0.2, 0.4, 0.6, 0.8, 0.99)]
        out = os.path.join(OUT, f"{name}-{'vet' if vet else 'take'}.jpg")
        print(shoot.sheet(picks, out, cols=3, w=640))
    return fs


SNOW_Y = 92.13  # top of the thin snow layer on the cleared set (snow_block at 91, layer at 92)


def follow_cam(a, b, frames, ground_y, dist=(3.5, 6.0), height=2.6, side=0.4, ahead=4.0, look_h=0.3, fov=62):
    """Lens keys for a camera BEHIND a subject walking a -> b (owner's brief: the subject walks into the
    beautiful background, we follow close behind and watch the prints appear at its feet).

    dist: how far behind at the start and at the end; growing distance lets the trail lengthen in the lower
    half of the frame. side: a little to the right so the body does not hide the newest prints. The lens looks
    `ahead` blocks past the subject, which keeps the horizon in the top third."""
    (x0, z0), (x1, z1) = a, b
    L = math.hypot(x1 - x0, z1 - z0)
    dx, dz = (x1 - x0) / L, (z1 - z0) / L
    rx, rz = -dz, dx  # to the subject's right
    keys = []
    for i in range(7):
        t = i / 6
        sx, sz = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
        d = dist[0] + (dist[1] - dist[0]) * t
        keys.append({"frame": int(frames * t),
                     "pos": [sx - dx * d + rx * side, ground_y + height, sz - dz * d + rz * side],
                     "look": [sx + dx * ahead, ground_y + look_h, sz + dz * ahead], "fov": fov, "ease": "linear"})
    return keys


def shot_snow_hero(vet=True):
    """The hook: the hero walks toward the sunlit mountain, we follow close behind and the trail grows."""
    frames = 420
    a, b = (1781.5, -1771.0), (1781.2, -1801.0)
    actor = {"name": "Imprint", "keys": walk_keys(a, b, frames)}
    cam = follow_cam(a, b, frames, SNOW_Y, dist=(3.2, 5.5), height=2.5)
    return vet_or_record("imprint_snow_hero", cam, frames, [], actors=[actor], vet=vet)


def shot_wolves(vet=True):
    """A wolf pack trots toward the mountain; we run behind it over a carpet of fresh paw prints."""
    frames = 300
    a, b = (1779.0, -1772.0), (1785.0, -1798.0)
    L = math.hypot(b[0] - a[0], b[1] - a[1])
    dx, dz = (b[0] - a[0]) / L, (b[1] - a[1]) / L
    rx, rz = -dz, dx
    setup, cues = [], []
    # loose formation: across the path and a little staggered along it
    for i, (across, along) in enumerate([(-1.2, -0.6), (0.0, 0.9), (1.2, -1.5)]):
        la = (a[0] + rx * across + dx * along, a[1] + rz * across + dz * along)
        lb = (b[0] + rx * across + dx * along, b[1] + rz * across + dz * along)
        tag = f"wolf{i}"
        setup.append(summon("wolf", tag, la, SNOW_Y, toward=lb))
        cues += mob_cues(f"@e[tag={tag},limit=1]", [la, lb], frames, SNOW_Y)
    cam = follow_cam(a, b, frames, SNOW_Y, dist=(5.2, 6.8), height=3.0, side=0.0, ahead=5.0)
    return vet_or_record("imprint_wolves", cam, frames, setup, cues=cues, vet=vet)


SAVANNA_Y = 88.0  # the red-sand flat at (224, 192); its terracotta top was turned into red sand for prints


def shot_savanna(vet=True):
    """Golden hour: the hero and a camel walk side by side toward the acacia hills, we follow behind."""
    frames = 360
    ha, hb = (214.4, 202.0), (214.7, 184.0)
    ca, cb = (210.9, 200.0), (211.2, 182.0)
    hero = {"name": "Imprint", "keys": walk_keys(ha, hb, frames)}
    setup = [summon("camel", "camel", ca, SAVANNA_Y, toward=cb)]
    cues = mob_cues("@e[tag=camel,limit=1]", [ca, cb], frames, SAVANNA_Y)
    mid_a, mid_b = (212.8, 201.5), (213.1, 183.5)
    cam = follow_cam(mid_a, mid_b, frames, SAVANNA_Y, dist=(5.6, 7.2), height=3.2, side=0.6, fov=62)
    return vet_or_record("imprint_savanna", cam, frames, setup, actors=[hero], cues=cues, vet=vet, tod=11000)


def shot_chicken(vet=True):
    """Macro: we tail a chicken at ground level, three-toed prints pop up right under the lens."""
    frames = 240
    a, b = (217.0, 199.0), (217.3, 191.0)
    setup = [summon("chicken", "chick", a, SAVANNA_Y, toward=b)]
    cues = mob_cues("@e[tag=chick,limit=1]", [a, b], frames, SAVANNA_Y)
    cam = follow_cam(a, b, frames, SAVANNA_Y, dist=(2.6, 3.6), height=1.9, side=0.3, ahead=1.5, look_h=0.0,
                     fov=54)
    return vet_or_record("imprint_chicken", cam, frames, setup, cues=cues, vet=vet, tod=11000)


def shot_polar_bear(vet=True):
    """Finale: a polar bear lumbers toward the sunlit cliff, we follow its trail of huge paw prints."""
    frames = 360
    a, b = (1780.0, -1775.0), (1792.0, -1800.0)
    setup = [summon("polar_bear", "bear", a, SNOW_Y, toward=b)]
    cues = mob_cues("@e[tag=bear,limit=1]", [a, b], frames, SNOW_Y)
    cam = follow_cam(a, b, frames, SNOW_Y, dist=(6.0, 7.5), height=3.2, side=2.4, ahead=5.0)
    return vet_or_record("imprint_polar_bear", cam, frames, setup, cues=cues, vet=vet)


def shot_gait(vet=True):
    """Check shot from above: a polar bear, a wolf and a camel walk side by side, so the gait of every trail
    (alternating left/right, the hind foot in the front print) can be read at a glance."""
    frames = 300
    setup, cues = [], []
    for i, (kind, x) in enumerate([("polar_bear", 1781.5), ("wolf", 1786.5), ("camel", 1791.5)]):
        a, b = (x, -1778.0), (x, -1796.0)
        setup.append(summon(kind, f"gait{i}", a, SNOW_Y, toward=b))
        cues += mob_cues(f"@e[tag=gait{i},limit=1]", [a, b], frames, SNOW_Y)
    key = {"pos": [1786.5, 106.0, -1783.0], "look": [1786.5, SNOW_Y, -1788.0], "fov": 60, "ease": "linear"}
    cam = [dict(key, frame=0), dict(key, frame=frames)]
    return vet_or_record("imprint_gait", cam, frames, setup, cues=cues, vet=vet)


SHOTS = {"gait": shot_gait, "snow_hero": shot_snow_hero, "wolves": shot_wolves, "savanna": shot_savanna, "chicken": shot_chicken,
         "polar_bear": shot_polar_bear}

if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "scout"
    record = "--record" in sys.argv
    if what in SHOTS:
        SHOTS[what](vet=not record)
    elif what == "ring":  # ring <x> <z> [tod] [label]
        ring(float(sys.argv[2]), float(sys.argv[3]), int(sys.argv[4]) if len(sys.argv) > 4 else 6000,
             label=sys.argv[5] if len(sys.argv) > 5 else "ring")
    else:
        {"scout": scout, "test": test}[what]()
