#!/usr/bin/env python3
"""Sample colours from, or crop regions of, a reference image.

Coordinates are pixels of the reference PNG itself (the files in
docs/design/reference are 1080x2424), not of a scaled-down preview.

  scripts/sample-colors.py docs/design/reference/hoje.png 159,1433 220,1436
  scripts/sample-colors.py IMG --box 220,1560,560,1610       # brightest pixels in a box (thin text)
  scripts/sample-colors.py IMG --crop 72,993,1006,1696 --out /tmp/x.png --scale 2

Requires Pillow (python3 -m pip install pillow).
"""
import argparse

from PIL import Image


def hex_of(rgb):
    return "#%02X%02X%02X" % rgb[:3]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("image")
    parser.add_argument("points", nargs="*", help="x,y pixel coordinates to sample (3x3 average)")
    parser.add_argument("--box", help="x0,y0,x1,y1: report the average of the brightest 5%% of pixels")
    parser.add_argument("--crop", help="x0,y0,x1,y1: save this region to --out")
    parser.add_argument("--out", help="output path for --crop")
    parser.add_argument("--scale", type=int, default=1, help="nearest-neighbour upscale for --crop")
    args = parser.parse_args()

    image = Image.open(args.image).convert("RGB")

    for point in args.points:
        x, y = (int(v) for v in point.split(","))
        pixels = [image.getpixel((x + dx, y + dy)) for dx in (-1, 0, 1) for dy in (-1, 0, 1)]
        avg = tuple(sum(p[i] for p in pixels) // len(pixels) for i in range(3))
        print(f"{point}: {hex_of(avg)}")

    if args.box:
        x0, y0, x1, y1 = (int(v) for v in args.box.split(","))
        region = image.crop((x0, y0, x1, y1))
        data = region.get_flattened_data() if hasattr(region, "get_flattened_data") else region.getdata()
        pixels = sorted(data, key=sum, reverse=True)
        top = pixels[: max(1, len(pixels) // 20)]
        avg = tuple(sum(p[i] for p in top) // len(top) for i in range(3))
        print(f"box {args.box}: {hex_of(avg)}")

    if args.crop:
        if not args.out:
            parser.error("--crop needs --out")
        x0, y0, x1, y1 = (int(v) for v in args.crop.split(","))
        region = image.crop((x0, y0, x1, y1))
        if args.scale != 1:
            region = region.resize((region.width * args.scale, region.height * args.scale), Image.NEAREST)
        region.save(args.out)
        print(f"saved {args.out} {region.size}")


if __name__ == "__main__":
    main()
