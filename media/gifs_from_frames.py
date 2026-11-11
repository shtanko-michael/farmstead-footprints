"""Recorded frames (scenes/gifs.py) -> media/gifs/<clip>.gif, 720 px wide, 20 fps, own palette per clip.

    python -I media/gifs_from_frames.py [target]      default target: 1.21.1-neoforge
"""
import subprocess
import sys
from pathlib import Path

import imageio_ffmpeg

ROOT = Path(__file__).resolve().parent.parent
target = sys.argv[1] if len(sys.argv) > 1 else "1.21.1-neoforge"
src = ROOT / "run" / target / "client" / "recordings"
out = ROOT / "media" / "gifs"
out.mkdir(parents=True, exist_ok=True)
ff = imageio_ffmpeg.get_ffmpeg_exe()
for clip in sorted(p for p in src.iterdir() if p.is_dir()):
    frames = sorted(clip.glob("*.png"))
    if not frames:
        continue
    gif = out / f"{clip.name}.gif"
    vf = "scale=720:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=200:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle"
    subprocess.run([ff, "-y", "-v", "error", "-framerate", "20", "-i", str(clip / "%05d.png"), "-vf", vf, "-loop", "0", str(gif)],
                   check=True)
    print(f"{gif.name}: {len(frames)} frames, {gif.stat().st_size // 1024} KB")
