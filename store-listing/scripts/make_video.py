#!/usr/bin/env python3
"""Builds the demo video, following store-listing/video/brag-plan.md (the brag-slim workflow).

Every frame is drawn from the app's real UI (the Robolectric captures in store-listing/source/,
see captures.py) with the app's fonts and colours; the soundtrack is synthesised here on one
120 BPM grid. Needs Pillow, NumPy and ffmpeg on PATH.

    python3 store-listing/scripts/make_video.py            # both languages
    python3 store-listing/scripts/make_video.py --still 2.2 --locale pt-PT   # one frame, for checks

pt-PT goes to docs/tempus-remedium.mp4 (plus the README's docs/demo.webp preview); en-US to
store-listing/video/tempus-remedium-en-US.mp4. Posters go to store-listing/video/.
"""

import argparse
import math
import subprocess
import wave
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from captures import FONTS, REPO, SOURCE, STORE, forms, load, pulse

W, H, FPS = 1920, 1080, 30
BPM = 120
BEAT = 60 / BPM
DURATION = 22.0
FRAMES = int(DURATION * FPS)
POSTER_TIME = 2.2
SR = 44100

# Scene boundaries, all on beats (see the storyboard).
HOOK, REVEAL, FORMS, REMIND, MISSED, OUTRO, END = 0.0, 2.5, 5.5, 9.0, 14.0, 18.5, DURATION
NOTIFY_AT, TAP_AT, TAKEN_AT, STOCK_AT = 9.5, 11.5, 12.0, 16.0

# TempusPalette.Light
BG = (0xF9, 0xFA, 0xFC)
BLOBS = [(0xC6, 0xE1, 0xFF), (0xDC, 0xE6, 0xF2), (0xB8, 0xE6, 0xFE), (0xCF, 0xEE, 0xFA)]
INK = (0x1A, 0x23, 0x2C)
INK_SOFT = (0x47, 0x51, 0x5C)
INK_MUTE = (0x5B, 0x64, 0x6F)
ACCENT = (0x1A, 0x60, 0x9E)
DOT = (0x04, 0x9F, 0xD6)
DANGER = (0xC2, 0x41, 0x3A)
BRAND = [(0x02, 0x6A, 0x9F), (0x12, 0x5A, 0x98), (0x00, 0x66, 0x8B)]

# Store copy per language; UI words (slot names, actions, forms) are the app's own strings.
COPY = {
    "pt-PT": {
        "hook": ["Esqueceu-se", "de uma toma?"],
        "reveal": ("TEMPUS REMEDIUM", "O que tomar hoje,", "e a que horas."),
        "forms": ("MEDICAMENTOS", "Comprimidos, inaladores,", "pensos, gotas e mais."),
        "form_names": ["Redondo", "Quadrado", "Cápsula", "Cápsula mole", "Inalador", "Penso transdérmico", "Spray nasal", "Gotas", "Injetável"],
        "remind": ("LEMBRETES", "Avisa à hora certa.", "Tomei, num toque."),
        "notification": ("Tempus Remedium", "agora", "Manhã · 08:00", "Metformina 500mg, Vitamina D 1000IU", "Tomei", "Adiar 10 min"),
        "missed": ("EM ATRASO", "Passou a hora?", "Fica a vermelho."),
        "stock": ("REPOR EM BREVE", "Está a acabar?", "Avisa antes de faltar."),
        "byline": "por Dra. Maria Cortês Ferreira · Médica de família",
        "output": REPO / "docs/tempus-remedium.mp4",
    },
    "en-US": {
        "hook": ["Missed", "a dose?"],
        "reveal": ("TEMPUS REMEDIUM", "What to take today,", "and when."),
        "forms": ("MEDICATIONS", "Pills, inhalers,", "patches, drops and more."),
        "form_names": ["Round", "Square", "Capsule", "Softgel", "Inhaler", "Transdermal patch", "Nasal spray", "Drops", "Injection"],
        "remind": ("REMINDERS", "Right on time.", "Taken, in one tap."),
        "notification": ("Tempus Remedium", "now", "Morning · 08:00", "Metformin 500mg, Vitamin D 1000IU", "Taken", "Snooze 10 min"),
        "missed": ("OVERDUE", "Time passed?", "It turns red."),
        "stock": ("REFILL SOON", "Running low?", "It warns you first."),
        "byline": "by Dr Maria Cortês Ferreira · Family doctor",
        "output": STORE / "video/tempus-remedium-en-US.mp4",
    },
}
LANGUAGES = "Português · English · Français · Deutsch · Español"
SITE = "mariacf.pt/apps"


# ---------------------------------------------------------------- helpers


def font(name, size):
    return ImageFont.truetype(str(FONTS / name), size)


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def ease_out(t):
    return 1 - (1 - clamp(t)) ** 3


def ease_in_out(t):
    t = clamp(t)
    return 4 * t**3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2


def ease_out_back(t):
    t = clamp(t)
    c = 1.4
    return 1 + (c + 1) * (t - 1) ** 3 + c * (t - 1) ** 2


def fast_out_slow_in(t):
    """Compose's default tween easing, CubicBezier(0.4, 0, 0.2, 1), solved numerically."""
    t = clamp(t)
    lo, hi = 0.0, 1.0
    for _ in range(24):
        u = (lo + hi) / 2
        x = 3 * (1 - u) ** 2 * u * 0.4 + 3 * (1 - u) * u**2 * 0.2 + u**3
        lo, hi = (u, hi) if x < t else (lo, u)
    u = (lo + hi) / 2
    return 3 * (1 - u) * u**2 + u**3


def enter_exit(t, start, end, fade_in=0.35, fade_out=0.25):
    """Opacity and slide (0..1) for content on screen from start to end: old out, then new in."""
    if t < start or t >= end:
        return 0.0, 0.0
    a = ease_out((t - start) / fade_in)
    b = 1 - ease_in_out((t - (end - fade_out)) / fade_out) if t > end - fade_out else 1.0
    return min(a, b), a


def rounded_mask(size, radius, scale=3):
    big = Image.new("L", (size[0] * scale, size[1] * scale), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, big.width - 1, big.height - 1), radius * scale, fill=255)
    return big.resize(size, Image.LANCZOS)


def with_opacity(layer, opacity):
    if opacity >= 0.999:
        return layer
    out = layer.copy()
    out.putalpha(layer.getchannel("A").point([int(v * opacity) for v in range(256)]))
    return out


def paste(canvas, layer, x, y, opacity=1.0):
    """Alpha-composites [layer] at (x, y), clipped to the canvas."""
    if opacity <= 0.003:
        return
    x, y = int(round(x)), int(round(y))
    left, top = max(0, -x), max(0, -y)
    right, bottom = min(layer.width, canvas.width - x), min(layer.height, canvas.height - y)
    if right <= left or bottom <= top:
        return
    part = layer.crop((left, top, right, bottom)) if (left, top, right, bottom) != (0, 0, layer.width, layer.height) else layer
    canvas.alpha_composite(with_opacity(part, opacity), (x + left, y + top))


def spaced_text(draw, xy, text, fnt, fill, spacing):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=fnt, fill=fill)
        x += draw.textlength(ch, font=fnt) + spacing
    return x


# ---------------------------------------------------------------- layers


@lru_cache(maxsize=1)
def background():
    """The app's light brand background: near-white with soft blue blobs, a little larger than the frame."""
    bw, bh = W + 120, H + 80
    small = Image.new("RGB", (bw // 4, bh // 4), BG)
    draw = ImageDraw.Draw(small)
    for (cx, cy, r), colour in zip([(0.12, 0.1, 0.42), (0.85, 0.2, 0.38), (0.7, 0.95, 0.45), (0.1, 0.9, 0.3)], BLOBS):
        w, h = small.size
        draw.ellipse(((cx - r) * w, cy * h - r * w, (cx + r) * w, cy * h + r * w), fill=colour)
    return small.filter(ImageFilter.GaussianBlur(70)).resize((bw, bh), Image.BICUBIC).convert("RGBA")


@lru_cache(maxsize=32)
def title_block(eyebrow, line1, line2, accent=ACCENT, size=84, max_width=880):
    """Eyebrow with the app's dot, a Fraunces line and an italic accent line, as on the Hoje header."""
    layer = Image.new("RGBA", (900, 330), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    y = 0
    if eyebrow:
        draw.ellipse((2, 13, 16, 27), fill=DOT if accent != DANGER else DANGER)
        spaced_text(draw, (34, 0), eyebrow, font("jakarta_bold.ttf", 28), INK_MUTE, 4)
        y = 64
    regular = font("fraunces_semibold.ttf", size)
    italic = font("fraunces_medium_italic.ttf", size)
    while max(draw.textlength(line1, font=regular), draw.textlength(line2, font=italic)) > max_width:
        size -= 4
        regular, italic = font("fraunces_semibold.ttf", size), font("fraunces_medium_italic.ttf", size)
    draw.text((0, y), line1, font=regular, fill=INK)
    draw.text((0, y + round(size * 1.12)), line2, font=italic, fill=accent)
    return layer.crop(layer.getbbox())


@lru_cache(maxsize=4)
def hook_text(lines):
    size = 104
    regular = font("fraunces_semibold.ttf", size)
    layer = Image.new("RGBA", (900, 300), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for i, line in enumerate(lines):
        draw.text((0, i * round(size * 1.1)), line, font=regular, fill=INK)
    return layer.crop(layer.getbbox())


SCREENS = {}


def screen(key, locale):
    if (key, locale) not in SCREENS:
        if key.startswith("pulse"):
            SCREENS[key, locale] = pulse(locale, int(key[5:]) / 100)
        else:
            SCREENS[key, locale] = load(locale, key)
    return SCREENS[key, locale]


@lru_cache(maxsize=40)
def phone(key, locale, scale):
    """The capture in a dark rounded phone body, with a soft shadow; returns (layer, screen offset)."""
    capture = screen(key, locale)
    sw, sh = round(capture.width * scale), round(capture.height * scale)
    bezel = max(8, round(26 * scale))
    inner, outer = round(100 * scale), round(100 * scale) + bezel
    pad = 60
    pw, ph = sw + 2 * bezel, sh + 2 * bezel
    layer = Image.new("RGBA", (pw + 2 * pad, ph + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("L", layer.size, 0)
    shadow.paste(rounded_mask((pw, ph), outer).point(lambda v: v * 0.30), (pad, pad + 24))
    shadow = shadow.filter(ImageFilter.GaussianBlur(30))
    layer.paste(Image.new("RGBA", layer.size, (1, 30, 60, 255)), (0, 0), shadow)
    body = Image.new("RGBA", (pw, ph), (0x14, 0x1A, 0x21, 255))
    layer.paste(body, (pad, pad), rounded_mask((pw, ph), outer))
    layer.paste(capture.resize((sw, sh), Image.LANCZOS), (pad + bezel, pad + bezel), rounded_mask((sw, sh), inner))
    return layer, (pad + bezel, pad + bezel)


def draw_phone(canvas, key, locale, scale, cx, top, opacity=1.0):
    layer, (ox, oy) = phone(key, locale, scale)
    x = cx - layer.width / 2
    paste(canvas, layer, x, top - oy, opacity)
    return x + ox, top  # screen origin on the canvas


@lru_cache(maxsize=2)
def hook_panel(locale):
    """HOJE, its four slots (MEIO-DIA in red) and the legend, cut from the 13:40 capture, as a card."""
    crop = screen("02_overdue_base", locale).crop((40, 410, 1041, 1030))
    scale = 0.78
    crop = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.LANCZOS)
    pad = 50
    layer = Image.new("RGBA", (crop.width + 2 * pad, crop.height + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("L", layer.size, 0)
    shadow.paste(rounded_mask(crop.size, 40).point(lambda v: v * 0.22), (pad, pad + 18))
    layer.paste(Image.new("RGBA", layer.size, (1, 40, 80, 255)), (0, 0), shadow.filter(ImageFilter.GaussianBlur(26)))
    layer.paste(crop, (pad, pad), rounded_mask(crop.size, 40))
    return layer


@lru_cache(maxsize=2)
def form_tiles(locale):
    names = COPY[locale]["form_names"]
    cutouts = forms()
    tiles = []
    label_font = font("jakarta_semibold.ttf", 22)
    for (key, icon), name in zip(cutouts.items(), names):
        tile = Image.new("RGBA", (210, 250), (0, 0, 0, 0))
        card = Image.new("RGBA", (190, 170), (255, 255, 255, 235))
        tile.paste(card, (10, 0), rounded_mask((190, 170), 34))
        h = 104
        w = round(icon.width * h / icon.height)
        if w > 150:
            w, h = 150, round(icon.height * 150 / icon.width)
        art = icon.resize((w, h), Image.LANCZOS)
        tile.alpha_composite(art, (105 - w // 2, 85 - h // 2))
        draw = ImageDraw.Draw(tile)
        fnt = label_font
        while draw.textlength(name, font=fnt) > 208:
            fnt = font("jakarta_semibold.ttf", fnt.size - 1)
        draw.text((105, 186), name, font=fnt, fill=INK_SOFT, anchor="ma")
        tiles.append(tile)
    return tiles


@lru_cache(maxsize=2)
def notification(locale, pressed):
    app, when, title, body, take, snooze = COPY[locale]["notification"]
    w, h = 620, 214
    layer = Image.new("RGBA", (w + 80, h + 80), (0, 0, 0, 0))
    shadow = Image.new("L", layer.size, 0)
    shadow.paste(rounded_mask((w, h), 34).point(lambda v: v * 0.28), (40, 54))
    layer.paste(Image.new("RGBA", layer.size, (1, 30, 60, 255)), (0, 0), shadow.filter(ImageFilter.GaussianBlur(22)))
    card = Image.new("RGBA", (w, h), (255, 255, 255, 255))
    layer.paste(card, (40, 40), rounded_mask((w, h), 34))
    draw = ImageDraw.Draw(layer)
    x0, y0 = 40 + 28, 40 + 24
    # App icon: the launcher mark (white scored tablet on the brand blue).
    draw.ellipse((x0, y0, x0 + 30, y0 + 30), fill=BRAND[1])
    draw.ellipse((x0 + 7, y0 + 7, x0 + 23, y0 + 23), fill=(255, 255, 255))
    draw.line((x0 + 11, y0 + 19, x0 + 19, y0 + 11), fill=BRAND[1], width=3)
    draw.text((x0 + 42, y0 + 4), f"{app} · {when}", font=font("jakarta_medium.ttf", 21), fill=INK_MUTE)
    draw.text((x0, y0 + 46), title, font=font("jakarta_bold.ttf", 28), fill=INK)
    draw.text((x0, y0 + 86), body, font=font("jakarta_regular.ttf", 24), fill=INK_SOFT)
    action = font("jakarta_bold.ttf", 24)
    if pressed:
        tw = draw.textlength(take, font=action)
        draw.rounded_rectangle((x0 - 12, y0 + 122, x0 + tw + 12, y0 + 166), 20, fill=(0xDE, 0xED, 0xFE))
    draw.text((x0, y0 + 130), take, font=action, fill=ACCENT)
    draw.text((x0 + draw.textlength(take, font=action) + 44, y0 + 130), snooze, font=action, fill=ACCENT)
    tap = (x0 + draw.textlength(take, font=action) / 2, y0 + 144)
    return layer, tap


@lru_cache(maxsize=2)
def outro(locale):
    layer = Image.new("RGBA", (W, 620), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    mark = 120
    gradient = Image.new("RGBA", (mark, mark))
    gd = ImageDraw.Draw(gradient)
    for i in range(mark * 2):
        t = i / (mark * 2)
        colour = tuple(round(a + (b - a) * t) for a, b in zip(BRAND[0], BRAND[2]))
        gd.line((i, 0, 0, i), fill=colour, width=2)
    tile = Image.new("RGBA", (mark, mark), (0, 0, 0, 0))
    tile.paste(gradient, (0, 0), rounded_mask((mark, mark), 38))
    td = ImageDraw.Draw(tile)
    td.ellipse((24, 24, 96, 96), fill=(255, 255, 255))
    td.line((44, 76, 76, 44), fill=BRAND[1], width=10)
    parts = {"mark": tile}
    title = Image.new("RGBA", (W, 160), (0, 0, 0, 0))
    ImageDraw.Draw(title).text((W / 2, 10), "Tempus Remedium", font=font("fraunces_semibold.ttf", 120), fill=INK, anchor="ma")
    parts["title"] = title
    byline = Image.new("RGBA", (W, 50), (0, 0, 0, 0))
    ImageDraw.Draw(byline).text((W / 2, 4), COPY[locale]["byline"], font=font("jakarta_medium.ttf", 34), fill=INK_SOFT, anchor="ma")
    parts["byline"] = byline
    languages = Image.new("RGBA", (W, 44), (0, 0, 0, 0))
    ImageDraw.Draw(languages).text((W / 2, 4), LANGUAGES, font=font("jakarta_medium.ttf", 28), fill=INK_MUTE, anchor="ma")
    parts["languages"] = languages
    button = Image.new("RGBA", (W, 90), (0, 0, 0, 0))
    bw = 380
    pill = Image.new("RGBA", (bw, 76), BRAND[1] + (255,))
    button.paste(pill, ((W - bw) // 2, 6), rounded_mask((bw, 76), 38))
    ImageDraw.Draw(button).text((W / 2, 44), SITE, font=font("jakarta_bold.ttf", 32), fill=(255, 255, 255), anchor="mm")
    parts["button"] = button
    return parts


# ---------------------------------------------------------------- scenes


def jar_pulse_key(t):
    """AlertGlow: alpha 0.15 -> 1 over 700 ms with FastOutSlowIn, reversing; quantised to 18 frames."""
    phase = (t % 1.4) / 0.7
    progress = fast_out_slow_in(phase if phase <= 1 else 2 - phase)
    value = 0.15 + 0.85 * progress
    return f"pulse{int(round(value * 20)) * 5:03d}" if value < 0.999 else "pulse100"


def text_left(canvas, layer, t, start, end, y=None):
    opacity, slide = enter_exit(t, start, end)
    if opacity <= 0:
        return
    y = (H - layer.height) / 2 if y is None else y
    paste(canvas, layer, 150, y + (1 - slide) * 36, opacity)


def render(t, locale):
    copy = COPY[locale]
    bg = background()
    drift = math.sin(t / DURATION * math.pi)
    canvas = bg.crop((int(30 + 40 * drift), int(20 + 20 * drift), int(30 + 40 * drift) + W, int(20 + 20 * drift) + H))

    # 1. Hook
    opacity, slide = enter_exit(t, HOOK, REVEAL, fade_in=0.4)
    if opacity > 0:
        layer = hook_text(tuple(copy["hook"]))
        paste(canvas, layer, 150, (H - layer.height) / 2 + (1 - slide) * 36, opacity)
        panel = hook_panel(locale)
        zoom = 1 + 0.05 * clamp(t / REVEAL)
        scaled = panel.resize((round(panel.width * zoom), round(panel.height * zoom)), Image.BILINEAR)
        paste(canvas, scaled, 1420 - scaled.width / 2, H / 2 - scaled.height / 2 + (1 - slide) * 50, opacity)

    # 2. Reveal: Hoje at 12:40
    opacity, slide = enter_exit(t, REVEAL, FORMS)
    if opacity > 0:
        text_left(canvas, title_block(*copy["reveal"]), t, REVEAL, FORMS)
        draw_phone(canvas, "01_today", locale, 0.4, 1390, 95 + (1 - slide) * 90, opacity)

    # 3. Every form, one per eighth note
    opacity, slide = enter_exit(t, FORMS, REMIND)
    if opacity > 0:
        text_left(canvas, title_block(*copy["forms"]), t, FORMS, REMIND)
        for i, tile in enumerate(form_tiles(locale)):
            appear = FORMS + BEAT / 2 + i * BEAT / 2
            if t < appear:
                continue
            pop = ease_out_back((t - appear) / 0.32)
            scaled = tile.resize((max(1, round(tile.width * (0.6 + 0.4 * pop))), max(1, round(tile.height * (0.6 + 0.4 * pop)))), Image.BILINEAR)
            cx = 1080 + (i % 3) * 230 + 105
            cy = 170 + (i // 3) * 262 + 125
            paste(canvas, scaled, cx - scaled.width / 2, cy - scaled.height / 2, opacity * clamp((t - appear) / 0.15))

    # 4. Reminder: notification, tap on "Tomei", MANHÃ taken
    opacity, slide = enter_exit(t, REMIND, MISSED)
    if opacity > 0:
        text_left(canvas, title_block(*copy["remind"]), t, REMIND, MISSED)
        top = 95 + (1 - slide) * 90
        draw_phone(canvas, "video_0759", locale, 0.4, 1390, top, opacity)
        if t > TAKEN_AT:
            draw_phone(canvas, "video_0801", locale, 0.4, 1390, top, opacity * ease_in_out((t - TAKEN_AT) / 0.4))
        if NOTIFY_AT <= t < TAKEN_AT + 0.35:
            pressed = TAP_AT <= t < TAKEN_AT
            card, (tx, ty) = notification(locale, pressed)
            enter = ease_out((t - NOTIFY_AT) / 0.4)
            leave = ease_in_out((t - TAKEN_AT) / 0.35) if t > TAKEN_AT else 0
            y = 40 - (1 - enter) * 120 - leave * 160
            x = 1390 - card.width / 2
            paste(canvas, card, x, y, opacity * enter * (1 - leave))
            if TAP_AT <= t < TAP_AT + 0.45:
                p = (t - TAP_AT) / 0.45
                ripple = Image.new("RGBA", (200, 200), (0, 0, 0, 0))
                r = 18 + 60 * ease_out(p)
                ImageDraw.Draw(ripple).ellipse((100 - r, 100 - r, 100 + r, 100 + r), fill=ACCENT + (int(90 * (1 - p)),))
                paste(canvas, ripple, x + tx - 100, y + ty - 100)

    # 5. Missed (red) and running low (the jar pulses); a close-up that pans from HOJE to the jars
    opacity, slide = enter_exit(t, MISSED, OUTRO)
    if opacity > 0:
        text_left(canvas, title_block(*copy["missed"], accent=DANGER, max_width=760), t, MISSED, STOCK_AT)
        text_left(canvas, title_block(*copy["stock"], accent=DANGER, max_width=760), t, STOCK_AT, OUTRO)
        scale = 0.78
        pan = ease_in_out((t - (STOCK_AT - 0.1)) / 0.6)
        focus = 690 + (1380 - 690) * pan
        draw_phone(canvas, jar_pulse_key(t - MISSED), locale, scale, 1460, H / 2 - focus * scale + (1 - slide) * 80, opacity)

    # 6. Outro
    if t >= OUTRO:
        parts = outro(locale)
        steps = [("mark", 230), ("title", 380), ("byline", 555), ("languages", 615), ("button", 685)]
        for i, (name, y) in enumerate(steps):
            start = OUTRO + i * BEAT / 2
            if t < start:
                continue
            k = ease_out((t - start) / 0.4)
            layer = parts[name]
            x = (W - layer.width) / 2
            paste(canvas, layer, x, y + (1 - k) * 30, k)
    return canvas.convert("RGB")


# ---------------------------------------------------------------- sound


def midi(note):
    return 440.0 * 2 ** ((note - 69) / 12)


CHORDS = [(50, 54, 57), (47, 50, 54), (43, 47, 50), (45, 49, 52)]  # D, Bm, G, A
ROOTS = [38, 35, 31, 33]


def soundtrack(path):
    n = int(DURATION * SR)
    t = np.arange(n) / SR
    left, right = np.zeros(n), np.zeros(n)
    rng = np.random.default_rng(7)

    def add(signal, start, gain, pan=0.0):
        i = int(round(start * SR))
        if i >= n:
            return
        s = signal[: n - i] * gain
        left[i : i + len(s)] += s * math.cos((pan + 1) * math.pi / 4)
        right[i : i + len(s)] += s * math.sin((pan + 1) * math.pi / 4)

    def tone(freq, length, decay, partials=((1, 1.0),), attack=0.004):
        x = np.arange(int(length * SR)) / SR
        wave_ = sum(a * np.sin(2 * np.pi * freq * m * x) for m, a in partials)
        env = np.minimum(1, x / attack) * np.exp(-x / decay)
        return wave_ * env

    def lowpass(x, cutoff):
        a = math.exp(-2 * math.pi * cutoff / SR)
        y = np.empty_like(x)
        acc = 0.0
        for i, v in enumerate(x):
            acc = (1 - a) * v + a * acc
            y[i] = acc
        return y

    bars = int(DURATION / (4 * BEAT)) + 1
    # Pad: soft saw-like chords, one per bar, filtered.
    pad = np.zeros(n)
    for b in range(bars):
        start, length = b * 4 * BEAT, 4 * BEAT + 0.6
        x = np.arange(int(length * SR)) / SR
        env = np.minimum(1, x / 0.35) * np.minimum(1, np.maximum(0, (length - x) / 0.6))
        chord = sum(sum(np.sin(2 * np.pi * midi(nt + 12) * k * x * (1 + d)) / k for k in range(1, 7)) for nt in CHORDS[b % 4] for d in (-0.002, 0.002))
        i = int(start * SR)
        seg = (chord * env)[: n - i]
        pad[i : i + len(seg)] += seg
    pad = lowpass(pad / 8, 900)

    # Kick, with the pad ducked under it, from the reveal; it rests in the outro's last bar.
    duck = np.ones(n)
    beats = np.arange(REVEAL, 20.0, BEAT)
    for bt in beats:
        x = np.arange(int(0.32 * SR)) / SR
        freq = 45 + 75 * np.exp(-x / 0.035)
        kick = np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-x / 0.12)
        add(kick, bt, 0.40)
        i = int(bt * SR)
        dl = min(len(x), n - i)
        duck[i : i + dl] = np.minimum(duck[i : i + dl], 1 - 0.35 * np.exp(-x[:dl] / 0.12))
    # The hook has no drums, so its pad and plucks play louder (until the reveal).
    intro = 1 + 0.6 * np.clip((REVEAL + 0.1 - t) / 0.2, 0, 1)
    add(pad * duck * intro, 0, 0.16, -0.1)
    add(pad * duck * intro, 0, 0.16, 0.1)

    # Bass on every beat, from the reveal.
    for bt in beats:
        root = ROOTS[int(bt // (4 * BEAT)) % 4]
        add(tone(midi(root), 0.45, 0.22, ((1, 1.0), (2, 0.25))), bt, 0.24)

    # Claps on 2 and 4, hats on the off-beats.
    for bt in np.arange(REVEAL, 20.0, BEAT / 2):
        beat_index = round(bt / BEAT * 2)
        noise = rng.standard_normal(int(0.2 * SR))
        if beat_index % 2 == 1:
            hat = np.diff(noise[: int(0.05 * SR)], prepend=0) * np.exp(-np.arange(int(0.05 * SR)) / SR / 0.012)
            add(hat, bt, 0.035, 0.35)
        elif (beat_index // 2) % 2 == 1 and bt >= 4.0:
            x = np.arange(len(noise)) / SR
            clap = np.diff(noise, prepend=0) * np.exp(-x / 0.05) * 0.5 + np.sin(2 * np.pi * 190 * x) * np.exp(-x / 0.03)
            add(clap, bt, 0.07, -0.05)

    # Plucks: arpeggio of the bar's chord on eighth notes, marimba-like.
    pattern = [0, 1, 2, 1, 2, 0, 1, 2]
    for b in range(bars):
        for k, idx in enumerate(pattern):
            start = b * 4 * BEAT + k * BEAT / 2
            if start >= 20.0:
                break
            note = CHORDS[b % 4][idx] + 24
            add(tone(midi(note), 0.5, 0.16, ((1, 1.0), (4, 0.12), (10, 0.03))), start, 0.16 if start < REVEAL else 0.10, -0.25)

    # Effects, in key and on the grid.
    pentatonic = [74, 76, 78, 81, 83, 86, 88, 90, 93]
    for i, note in enumerate(pentatonic):
        add(tone(midi(note), 0.25, 0.06, ((1, 1.0), (2, 0.2))), FORMS + BEAT / 2 + i * BEAT / 2, 0.12, -0.3 + 0.075 * i)
    bell = ((1, 1.0), (2.76, 0.35), (5.4, 0.12))
    add(tone(midi(81), 1.2, 0.35, bell), NOTIFY_AT, 0.16, 0.2)
    add(tone(midi(86), 1.4, 0.45, bell), NOTIFY_AT + BEAT / 2, 0.16, 0.2)
    click = rng.standard_normal(int(0.02 * SR)) * np.exp(-np.arange(int(0.02 * SR)) / SR / 0.002)
    add(click + tone(1760, 0.02, 0.004), TAP_AT, 0.12, 0.2)
    add(tone(midi(86), 0.6, 0.2, ((1, 1.0), (2, 0.2))), TAKEN_AT, 0.10, 0.2)
    for at in (MISSED, STOCK_AT):
        add(tone(midi(50), 0.9, 0.3, ((1, 1.0), (4, 0.15), (9.8, 0.04))), at, 0.18)
    for at in (REVEAL, FORMS, REMIND, MISSED, OUTRO):
        x = np.arange(int(0.4 * SR)) / SR
        swell = lowpass(lowpass(rng.standard_normal(len(x)), 1500), 1500) * np.sin(np.pi * x / 0.4) ** 2
        add(swell, at - 0.3, 0.06)
    for i, note in enumerate([86, 90, 93, 98]):
        add(tone(midi(note), 1.6, 0.5, bell), OUTRO + i * BEAT / 2, 0.09, -0.2 + 0.13 * i)
    # Final chord.
    for note in (62, 66, 69, 74):
        add(tone(midi(note), 3.0, 1.1, ((1, 1.0), (2, 0.2), (3, 0.05)), attack=0.02), 20.0, 0.09)

    mix = np.stack([left, right], axis=1)
    mix = np.tanh(mix * 1.3) / np.tanh(1.3)
    fade = np.ones(n)
    fade[: int(0.02 * SR)] = np.linspace(0, 1, int(0.02 * SR))
    tail = int((DURATION - 20.6) * SR)
    fade[-tail:] = np.linspace(1, 0, tail) ** 1.5
    mix *= fade[:, None]
    mix *= 0.89 / np.abs(mix).max()
    with wave.open(str(path), "wb") as out:
        out.setnchannels(2)
        out.setsampwidth(2)
        out.setframerate(SR)
        out.writeframes((mix * 32767).astype("<i2").tobytes())


# ---------------------------------------------------------------- output


def build(locale, work):
    work.mkdir(parents=True, exist_ok=True)
    audio = work / "soundtrack.wav"
    if not audio.exists():
        soundtrack(audio)
    output = COPY[locale]["output"]
    output.parent.mkdir(parents=True, exist_ok=True)
    poster = render(POSTER_TIME, locale)
    poster.save(STORE / f"video/poster-{locale}.jpg", quality=90)
    preview = []
    command = [
        "ffmpeg", "-v", "error", "-y",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
        "-i", str(audio),
        "-c:v", "libx264", "-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "160k", "-movflags", "+faststart", "-shortest", str(output),
    ]  # fmt: skip
    encoder = subprocess.Popen(command, stdin=subprocess.PIPE)
    for frame in range(FRAMES):
        # Frame 0 is the poster, so every platform's thumbnail shows a settled frame.
        image = poster if frame == 0 else render(frame / FPS, locale)
        encoder.stdin.write(image.tobytes())
        if locale == "pt-PT" and frame % 3 == 0:
            preview.append(image.resize((960, 540), Image.LANCZOS))
    encoder.stdin.close()
    if encoder.wait() != 0:
        raise SystemExit("ffmpeg failed")
    if preview:
        preview[0].save(REPO / "docs/demo.webp", save_all=True, append_images=preview[1:], duration=100, loop=0, quality=70, method=4)
    print(f"{locale}: {output.relative_to(REPO)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--locale", choices=list(COPY), action="append")
    parser.add_argument("--still", type=float, nargs="*", help="render these times as PNGs into source/video-work instead")
    args = parser.parse_args()
    work = SOURCE / "video-work"
    for locale in args.locale or list(COPY):
        if args.still is not None:
            work.mkdir(parents=True, exist_ok=True)
            for t in args.still:
                render(t, locale).save(work / f"still-{locale}-{t:05.2f}.png")
            continue
        build(locale, work)


if __name__ == "__main__":
    main()
