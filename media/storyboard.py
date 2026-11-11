"""Storyboard of the vetted shots: two frames per shot, in reel order -> media/out/storyboard.jpg

    python -I media/storyboard.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import film  # noqa: E402  (also puts the rig on the path)
import shoot  # noqa: E402

# reel order: scene, the two moments that sell it, caption
BOARD = [
    ("footprints_snow_hero", (0.5, 0.95), "1 Hook - we follow the hero toward the mountain"),
    ("footprints_wolves", (0.5, 0.85), "2 Wolf pack - behind the pack, paw prints underfoot"),
    ("footprints_savanna", (0.4, 0.8), "3 Savanna - camel and hero walk to the acacias"),
    ("footprints_chicken", (0.7, 0.95), "4 Macro - tailing a chicken at ground level"),
    ("footprints_polar_bear", (0.6, 0.95), "5 Finale - polar bear toward the sunlit cliff"),
]

files, labels = [], []
for scene, picks, caption in BOARD:
    fs = shoot.frames_of(scene)
    if not fs:
        print("no frames for", scene)
        continue
    for f in picks:
        files.append(fs[min(len(fs) - 1, int(len(fs) * f))])
        labels.append(caption)
print(shoot.sheet(files, os.path.join(film.OUT, "storyboard.jpg"), cols=2, w=800, labels=labels))
