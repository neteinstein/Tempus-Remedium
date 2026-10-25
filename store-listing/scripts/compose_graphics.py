#!/usr/bin/env python3
"""Turns the raw captures in store-listing/source/ into the files the Play Console takes.

The captures come from StoreListingCaptures (androidApp/src/test/.../store/), which renders the
real app with Robolectric. This script:

- frames each phone screen on the brand gradient with a two-line caption (captions.json), in the
  app's fonts, as a 1080 x 1920 RGB PNG (9:16, no alpha); 02_overdue shows the running-out glow at
  its brightest;
- flattens the feature graphic to a 1024 x 500 RGB PNG;
- copies the 512 x 512 icon as a 32-bit PNG.

Output goes to store-listing/listings/<locale>/graphics/, the layout Gradle Play Publisher reads.
Needs Pillow (pip install pillow). Run from anywhere: python3 store-listing/scripts/compose_graphics.py
"""

import json

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from captures import FONTS, SOURCE, STORE, load, pulse

WIDTH, HEIGHT = 1080, 1920
SCREEN_WIDTH = 760
SCREEN_BOTTOM_MARGIN = 70
CAPTION_TOP = 84
CAPTION_MAX_WIDTH = 940
CORNER = 46

# mariacf.pt brand gradient (TempusPalette.brand) and the caption colours of the feature graphic.
BRAND = [(0x02, 0x6A, 0x9F), (0x12, 0x5A, 0x98), (0x00, 0x66, 0x8B)]
INK = (255, 255, 255)
ACCENT = (0xBF, 0xE2, 0xFF)


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def brand_background(size):
    """Diagonal brand gradient with two soft white glows, like the feature graphic."""
    w, h = size
    small = Image.new("RGB", (w // 8, h // 8))
    px = small.load()
    sw, sh = small.size
    for y in range(sh):
        for x in range(sw):
            t = (x / sw + y / sh) / 2
            px[x, y] = lerp(BRAND[0], BRAND[1], t / 0.55) if t < 0.55 else lerp(BRAND[1], BRAND[2], (t - 0.55) / 0.45)
    image = small.resize(size, Image.BICUBIC)

    glow = Image.new("L", size, 0)
    draw = ImageDraw.Draw(glow)
    draw.ellipse((-0.35 * w, -0.12 * h, 0.75 * w, 0.45 * h), fill=46)
    draw.ellipse((0.45 * w, 0.62 * h, 1.4 * w, 1.25 * h), fill=34)
    glow = glow.filter(ImageFilter.GaussianBlur(w * 0.12))
    return Image.composite(Image.new("RGB", size, INK), image, glow)


def rounded_mask(size, radius, scale=4):
    big = Image.new("L", (size[0] * scale, size[1] * scale), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, big.width - 1, big.height - 1), radius * scale, fill=255)
    return big.resize(size, Image.LANCZOS)


def fit_fonts(lines, draw):
    """Largest size (<= 76 px) at which both caption lines fit CAPTION_MAX_WIDTH."""
    for size in range(76, 40, -2):
        regular = ImageFont.truetype(str(FONTS / "fraunces_semibold.ttf"), size)
        italic = ImageFont.truetype(str(FONTS / "fraunces_medium_italic.ttf"), size)
        widths = [draw.textlength(lines[0], font=regular), draw.textlength(lines[1], font=italic)]
        if max(widths) <= CAPTION_MAX_WIDTH:
            return regular, italic, size
    raise ValueError(f"Caption too long: {lines}")


def frame(screen, lines, out_path):
    canvas = brand_background((WIDTH, HEIGHT))
    draw = ImageDraw.Draw(canvas)

    regular, italic, size = fit_fonts(lines, draw)
    line_height = round(size * 1.14)
    for i, (text, font, colour) in enumerate([(lines[0], regular, INK), (lines[1], italic, ACCENT)]):
        draw.text((WIDTH / 2, CAPTION_TOP + i * line_height), text, font=font, fill=colour, anchor="ma")

    height = round(screen.height * SCREEN_WIDTH / screen.width)
    screen = screen.resize((SCREEN_WIDTH, height), Image.LANCZOS)
    x = (WIDTH - SCREEN_WIDTH) // 2
    y = HEIGHT - height - SCREEN_BOTTOM_MARGIN
    mask = rounded_mask(screen.size, CORNER)

    shadow = Image.new("L", canvas.size, 0)
    shadow.paste(mask.point(lambda v: v * 0.42), (x, y + 22))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34))
    canvas = Image.composite(Image.new("RGB", canvas.size, (0, 18, 40)), canvas, shadow)

    border = Image.new("RGB", (SCREEN_WIDTH + 6, height + 6), (255, 255, 255))
    canvas.paste(border, (x - 3, y - 3), rounded_mask(border.size, CORNER + 3).point(lambda v: v * 0.35))
    canvas.paste(screen, (x, y), mask)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out_path, optimize=True)


def main():
    captions = json.loads((STORE / "scripts/captions.json").read_text(encoding="utf-8"))
    for locale, screens in captions.items():
        graphics = STORE / "listings" / locale / "graphics"
        shots = graphics / "phone-screenshots"
        for old in shots.glob("*.png"):
            old.unlink()
        for index, (name, lines) in enumerate(screens.items(), start=1):
            screen = pulse(locale, 1.0) if name == "02_overdue" else load(locale, name)
            frame(screen, lines, shots / f"{index}.png")

        feature = load(locale, "feature_graphic")
        assert feature.size == (1024, 500), feature.size
        (graphics / "feature-graphic").mkdir(parents=True, exist_ok=True)
        feature.save(graphics / "feature-graphic/1.png", optimize=True)

        icon = Image.open(SOURCE / "icon.png").convert("RGBA")
        assert icon.size == (512, 512), icon.size
        (graphics / "icon").mkdir(parents=True, exist_ok=True)
        icon.save(graphics / "icon/1.png", optimize=True)
        print(f"{locale}: {len(screens)} screenshots, feature graphic, icon")


if __name__ == "__main__":
    main()
