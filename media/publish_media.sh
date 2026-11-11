#!/usr/bin/env bash
# Rebuild every published picture and video from the recorded takes (media/record_all.py first).
#   bash media/publish_media.sh
# Trailer, banner, logo and card are Remotion compositions in minecraft-api/video (src/mods/Footprints.tsx).
set -euo pipefail
HERE=$(cd "$(dirname "$0")/.." && pwd)
VIDEO=C:/Users/admin/minecraft-api/video
CO=C:/Users/admin/minecraft-api/game/capture-client/cinematic-out
FF=$(python -I -c "import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())")
TAKES="$HERE/media/out/takes"
MEDIA="$HERE/docs/media"

mkdir -p "$VIDEO/public/mods/footprints/takes" "$MEDIA/gallery" "$MEDIA/gifs"
cp "$TAKES"/*.mp4 "$VIDEO/public/mods/footprints/takes/"
cp "$MEDIA/icon-512.png" "$VIDEO/public/mods/footprints/icon.png"
cp "$MEDIA/icon-cutout.png" "$VIDEO/public/mods/footprints/icon-cutout.png"
cp "$CO/footprints_snow_hero/f_00400.jpg" "$VIDEO/public/mods/footprints/card-bg.jpg"

cd "$VIDEO"
npx remotion render src/index.ts FootprintsTrailer out/footprints/footprints-trailer.mp4 --codec=h264 --crf=18 --concurrency=8 --log=error
npx remotion render src/index.ts FootprintsBanner out/footprints/banner.mp4 --codec=h264 --crf=16 --concurrency=8 --log=error
npx remotion still src/index.ts FootprintsLogo "$MEDIA/logo.png" --image-format=png --log=error
npx remotion still src/index.ts FootprintsCard "$MEDIA/social-card.png" --log=error
"$FF" -v error -y -i out/footprints/footprints-trailer.mp4 -c:v libx264 -preset slow -crf 26 -r 30 -pix_fmt yuv420p \
  -c:a aac -b:a 160k -movflags +faststart out/footprints/footprints-trailer-preview.mp4

cd "$MEDIA"
"$FF" -v error -y -i "$VIDEO/out/footprints/banner.mp4" -vf fps=30 -c:v libwebp_anim -q:v 72 -loop 0 -an banner.webp
"$FF" -v error -y -i "$VIDEO/out/footprints/banner.mp4" \
  -vf "fps=12,scale=800:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4" \
  -loop 0 banner.gif
gif() {  # name, start frame, seconds
  "$FF" -v error -y -ss "$(python -I -c "print($2/60)")" -t "$3" -i "$TAKES/$1.mp4" \
    -vf "fps=12,scale=520:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4" \
    -loop 0 "gifs/$1.gif"
}
gif snow_hero 150 3
gif wolves 100 3
gif chicken 60 2.6
gif gait 130 2.8
gif savanna 120 2.6

python -I - "$CO" <<'PY'
import sys
from PIL import Image
co = sys.argv[1]
picks = [("01-snow-trail", "footprints_snow_hero", 330), ("02-wolf-pack", "footprints_wolves", 220),
         ("03-savanna", "footprints_savanna", 240), ("04-chicken", "footprints_chicken", 200),
         ("05-polar-bear", "footprints_polar_bear", 260), ("06-gait-top-view", "footprints_gait", 300)]
for out, scene, f in picks:
    Image.open(f"{co}/{scene}/f_{f:05d}.jpg").convert("RGB").save(f"gallery/{out}.jpg", quality=90)
PY
ls -la . gallery gifs
