#!/usr/bin/env python3
"""Checks store-listing/ against the Play Console limits before uploading.

Text: title <= 30, short description <= 80, full description <= 4000, release notes <= 500
characters. Images: icon 512 x 512 PNG (alpha allowed, <= 1 MB); feature graphic 1024 x 500 without
alpha; 2-8 phone screenshots without alpha, sides 320-3840 px, long side <= 2 x short side, <= 8 MB;
up to 8 tablet screenshots per size, 16:9 or 9:16, sides 320-3840 px (7") or 1080-7680 px (10").
Needs Pillow. Exits non-zero on the first failed check of each file, after listing them all.
"""

import sys
from pathlib import Path

from PIL import Image

STORE = Path(__file__).resolve().parent.parent
TEXT_LIMITS = {"title.txt": 30, "short-description.txt": 80, "full-description.txt": 4000}
NOTES_LIMIT = 500
# Folder -> (shortest side, longest side) allowed for that tablet size.
TABLET_SIDES = {"tablet-screenshots": (320, 3840), "large-tablet-screenshots": (1080, 7680)}

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)
    return condition


def check_text(path, limit):
    text = path.read_text(encoding="utf-8").strip()
    check(text, f"{path}: empty")
    check(len(text) <= limit, f"{path}: {len(text)} characters, limit {limit}")
    return len(text)


def check_image(path, size=None, alpha_ok=False, max_bytes=8 * 1024 * 1024, sides=None):
    with Image.open(path) as image:
        check(image.format == "PNG", f"{path}: {image.format}, expected PNG")
        w, h = image.size
        if size:
            check(image.size == size, f"{path}: {image.size}, expected {size}")
        elif sides:
            low, high = sides
            check(low <= min(w, h) and max(w, h) <= high, f"{path}: {image.size} outside {low}-{high} px")
            check(max(w, h) * 9 == min(w, h) * 16, f"{path}: not 16:9 or 9:16")
        else:
            check(320 <= min(w, h) and max(w, h) <= 3840, f"{path}: {image.size} outside 320-3840 px")
            check(max(w, h) <= 2 * min(w, h), f"{path}: aspect ratio over 2:1")
        if not alpha_ok:
            check("A" not in image.getbands(), f"{path}: has an alpha channel")
        check(path.stat().st_size <= max_bytes, f"{path}: {path.stat().st_size} bytes, limit {max_bytes}")


def main():
    for listing in sorted((STORE / "listings").iterdir()):
        counts = {name: check_text(listing / name, limit) for name, limit in TEXT_LIMITS.items()}
        graphics = listing / "graphics"
        check_image(graphics / "icon/1.png", (512, 512), alpha_ok=True, max_bytes=1024 * 1024)
        check_image(graphics / "feature-graphic/1.png", (1024, 500), max_bytes=15 * 1024 * 1024)
        shots = sorted((graphics / "phone-screenshots").glob("*.png"))
        check(2 <= len(shots) <= 8, f"{listing.name}: {len(shots)} phone screenshots, need 2-8")
        for shot in shots:
            check_image(shot)
        tablets = []
        for folder, sides in TABLET_SIDES.items():
            tablet_shots = sorted((graphics / folder).glob("*.png"))
            check(len(tablet_shots) <= 8, f"{listing.name}: {len(tablet_shots)} {folder}, at most 8")
            for shot in tablet_shots:
                check_image(shot, sides=sides)
            tablets.append(str(len(tablet_shots)))
        notes = STORE / "release-notes" / listing.name / "default.txt"
        counts["release notes"] = check_text(notes, NOTES_LIMIT)
        print(listing.name, ", ".join(f"{name} {count}" for name, count in counts.items()), f"screenshots {len(shots)}, tablet {' + '.join(tablets)}")

    for problem in problems:
        print("FAIL", problem)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
