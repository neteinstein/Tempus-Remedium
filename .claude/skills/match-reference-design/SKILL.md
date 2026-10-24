---
name: match-reference-design
description: Render a Compose component and make it match a reference image in docs/design/reference (layout, sizes, colours, exact pt-PT strings). Use when building or changing the HOJE or REPOR EM BREVE panels, pills, jars or widgets, or when asked to "match the screenshot".
---

# Match a reference design

The reference images in `docs/design/reference/` are the source of truth; `docs/design/REFERENCE.md`
transcribes them (strings, measurements, colours, deviations). Never "clean up" or invent content
that is not in the reference — copy strings character for character.

## Loop

1. **Read the spec.** Open the relevant section of `docs/design/REFERENCE.md` and look at the
   reference image. Note what is *not* part of the design (system clock, viewer overlay, photo).
2. **Measure, don't guess.** Coordinates are reference pixels (1080 px wide = 360 dp at 3×).
   ```bash
   python3 -I scripts/sample-colors.py docs/design/reference/hoje.png 140,1418          # colour at a point
   python3 -I scripts/sample-colors.py docs/design/reference/hoje.png --box 95,1000,240,1050   # thin text
   python3 -I scripts/sample-colors.py docs/design/reference/hoje.png --crop 72,990,1008,1720 --out /tmp/crop.png --scale 2
   ```
3. **Render.** Screenshot tests live in
   `androidApp/src/test/kotlin/.../screenshots/` and run on Robolectric at
   `pt-rPT-w360dp-h808dp-xxhdpi`, the same 1080 px width as the reference:
   ```bash
   ./gradlew :androidApp:recordRoborazziDebug
   ```
   Output: `androidApp/src/test/screenshots/*.png` (these are the committed goldens).
4. **Compare side by side** (reference left, render right) and look at the result:
   ```bash
   python3 -I scripts/compare-reference.py hoje /tmp/cmp_hoje.png
   python3 -I scripts/compare-reference.py repor-em-breve /tmp/cmp_repor.png
   ```
   Check, in this order: strings, element order, vertical rhythm (gaps between title, container,
   cards, legend), sizes (text cap height, pill and jar size), colours.
5. **Adjust** dimensions in `ui/.../components/*Panel.kt`, styles in `ui/.../theme/Theme.kt`,
   pill/jar geometry in `ui/.../pill/*Painter.kt`. Pills and jars are shared with the Android
   widgets (rendered to bitmaps), so a painter change affects both.
6. **Repeat** until the comparison matches, then record the final goldens and update
   `docs/design/REFERENCE.md` if you measured something new or added a deliberate deviation.

## Rules

- Data for these renders comes from `ReferenceFixtures` (core); keep it reproducing the reference.
- Any visual difference you introduce on purpose goes in the "Deliberate deviations" table.
- Text sizes are in `sp` and must keep scaling with the user's font size.
- `./gradlew :androidApp:verifyRoborazziDebug` is what CI runs; commit re-recorded goldens with
  the change that caused them.
