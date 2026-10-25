#!/usr/bin/env python3
"""Checks store-listing/ against the Play Console limits before uploading.

Text: title <= 30, short description <= 80, full description <= 4000, release notes <= 500
characters. Images: icon 512 x 512 PNG (alpha allowed, <= 1 MB); feature graphic 1024 x 500 without
alpha; 2-8 phone screenshots without alpha, sides 320-3840 px, long side <= 2 x short side, <= 8 MB.
Needs Pillow. Exits non-zero on the first failed check of each file, after listing them all.
"""

import sys
from pathlib import Path

from PIL import Image

STORE = Path(__file__).resolve().parent.parent
TEXT_LIMITS = {"title.txt": 30, "short-description.txt": 80, "full-description.txt": 4000}
NOTES_LIMIT = 500

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


def check_image(path, size=None, alpha_ok=False, max_bytes=8 * 1024 * 1024):
    with Image.open(path) as image:
        check(image.format == "PNG", f"{path}: {image.format}, expected PNG")
        if size:
            check(image.size == size, f"{path}: {image.size}, expected {size}")
        else:
            w, h = image.size
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
        notes = STORE / "release-notes" / listing.name / "default.txt"
        counts["release notes"] = check_text(notes, NOTES_LIMIT)
        print(listing.name, ", ".join(f"{name} {count}" for name, count in counts.items()), f"screenshots {len(shots)}")

    for problem in problems:
        print("FAIL", problem)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
