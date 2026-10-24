#!/usr/bin/env python3
"""Put a crop of a reference image next to a rendered screenshot.

  scripts/compare-reference.py hoje OUT.png
  scripts/compare-reference.py repor-em-breve OUT.png

The reference crop and the matching Roborazzi output (androidApp/src/test/screenshots)
are scaled to the same width and placed side by side, reference on the left. The dark-theme
renders are used because they sit closest to the reference's dark photo background; compare
layout, sizes and strings, not the surface colours (those follow the author's brand).
Requires Pillow.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent

# name: (reference file, crop box in reference pixels, rendered screenshot)
PANELS = {
    "hoje": ("hoje.png", (0, 990, 1080, 1720), "hoje_panel_dark.png"),
    "repor-em-breve": ("repor-em-breve.png", (0, 980, 1080, 1630), "repor_em_breve_panel_dark.png"),
}


def main():
    if len(sys.argv) != 3 or sys.argv[1] not in PANELS:
        sys.exit(__doc__)
    reference_name, box, rendered_name = PANELS[sys.argv[1]]
    reference = Image.open(ROOT / "docs/design/reference" / reference_name).convert("RGB").crop(box)
    rendered = Image.open(ROOT / "androidApp/src/test/screenshots" / rendered_name).convert("RGB")
    width = reference.width
    rendered = rendered.resize((width, round(rendered.height * width / rendered.width)))
    out = Image.new("RGB", (width * 2 + 20, max(reference.height, rendered.height)), "white")
    out.paste(reference, (0, 0))
    out.paste(rendered, (width + 20, 0))
    out.save(sys.argv[2])
    print(f"saved {sys.argv[2]}")


if __name__ == "__main__":
    main()
