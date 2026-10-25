"""Reads the raw captures StoreListingCaptures writes to store-listing/source/.

Shared by compose_graphics.py (store graphics) and make_video.py (demo video).
"""

from pathlib import Path

import numpy as np
from PIL import Image

STORE = Path(__file__).resolve().parent.parent
REPO = STORE.parent
SOURCE = STORE / "source"
FONTS = REPO / "ui/src/commonMain/composeResources/font"

# AlertGlow in ReporEmBrevePanel.kt animates its alpha from 0.15 to 1; tests never run infinite
# animations, so 02_overdue holds the glow at 0.15 and 02_overdue_base has no glow at all.
PULSE_START = 0.15

# PillShape.entries order, as laid out by formsOnWhite / formsOnBlack (3 x 3 cells of 360 px).
FORMS = ["round", "square", "capsule", "softgel", "inhaler", "patch", "nasal_spray", "drops", "injection"]
FORM_CELL = 360


def load(locale, name):
    return Image.open(SOURCE / locale / f"{name}.png").convert("RGB")


def pulse(locale, value):
    """02_overdue with the running-out glow at [value] (0.15 to 1), exact because it is alpha-blended."""
    glow = np.asarray(load(locale, "02_overdue"), dtype=np.float32)
    base = np.asarray(load(locale, "02_overdue_base"), dtype=np.float32)
    frame = base + (glow - base) * (value / PULSE_START)
    return Image.fromarray(np.clip(frame + 0.5, 0, 255).astype(np.uint8))


def forms():
    """Each medication form as an RGBA cut-out, recovered from the white and black renders."""
    white = np.asarray(Image.open(SOURCE / "forms_white.png").convert("RGB"), dtype=np.float32)
    black = np.asarray(Image.open(SOURCE / "forms_black.png").convert("RGB"), dtype=np.float32)
    alpha = np.clip(1 - (white - black).mean(axis=2) / 255, 0, 1)
    colour = np.where(alpha[..., None] > 0.004, black / np.maximum(alpha[..., None], 0.004), 0)
    rgba = np.dstack([np.clip(colour, 0, 255), alpha * 255]).astype(np.uint8)
    image = Image.fromarray(rgba, "RGBA")
    cutouts = {}
    for index, name in enumerate(FORMS):
        x, y = (index % 3) * FORM_CELL, (index // 3) * FORM_CELL
        cell = image.crop((x, y, x + FORM_CELL, y + FORM_CELL))
        cutouts[name] = cell.crop(cell.getbbox())
    return cutouts
