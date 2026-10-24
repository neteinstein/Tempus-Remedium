# Design reference

The two screenshots in [`reference/`](reference) are the **source of truth** for the structure
and content of the HOJE and REPOR EM BREVE panels: what they say, their layout, the pills and
the jars. They were provided directly by the project owner and have no origin URL.

Colours (other than the pills and the amber of "Restam N"), surfaces and typography follow the
author's site, [mariacf.pt](https://mariacf.pt), at the owner's request; see AGENTS.md →
"Author and visual identity". The screenshot tests render each panel in the light and the dark
theme; the dark renders are the ones to put next to the reference.

| File | Size | Shows |
| --- | --- | --- |
| [`reference/hoje.png`](reference/hoje.png) | 1080 × 2424 px | The **HOJE** panel: today's doses by time of day. |
| [`reference/repor-em-breve.png`](reference/repor-em-breve.png) | 1080 × 2424 px | The **REPOR EM BREVE** panel: medications running low. |

Pixel coordinates below are in the reference files themselves (1080 px wide = 360 dp at 3×).
Use `scripts/sample-colors.py` to sample them and `scripts/compare-reference.py` to put a
rendered screenshot next to them (see `.claude/skills/match-reference-design`).

## Rules

- **Copy strings exactly.** Accents, case, punctuation and spacing are part of the design:
  `MANHÃ`, `MEIO-DIA`, `Ómega 3`, `qui., 22 · 5 doses` (middle dot, spaces around it),
  `Restam 3`. Do not "fix" `1000IU` to `1000 UI`, `Restam` to `Faltam`, or reorder the legend.
- **Do not invent content** in these panels: no extra badges, icons, progress bars or copy
  that is not in the reference. Additions elsewhere in the app are fine and are listed below.
- pt-PT is authoritative. English strings are derived translations (see the table at the end).

## Not part of the design

These belong to the phone or to whatever the screenshots were captured in. Do not reproduce them:

- the system status bar, the lock-screen date (`quinta-feira, 22 de outubro`) and clock
  (`16:37`, `16:40`), the flashlight and camera buttons and the home indicator;
- the overlay of the viewer the screenshots were taken in: the round ✕ button (top left),
  the ⋯ button (top right) and the row of counters along the bottom (`55`, `4`, `3` and a send arrow);
- the photo behind the panels (a street with a tram). The app uses a warm dark gradient instead.

## HOJE panel (`hoje.png`)

| Element | Content (exact) | Notes |
| --- | --- | --- |
| Title | `HOJE` | Uppercase, semibold, wide letter spacing. Cap height ≈ 29 px (y 1015–1044). |
| Title meta | `qui., 22 · 5 doses` | Regular, ~80 % white. Abbreviated weekday + day of month · number of doses. |
| Glass container | — | x 73–1005, y 1080–1528. Rounded (~22 dp), white fill ~10 %, 1 dp white stroke ~30 %. |
| Slot cards | `MANHÃ 08:00`, `MEIO-DIA 13:00`, `NOITE 19:00`, `DEITAR 22:00` | Four equal cards, y 1110–1497, ~22 px gaps. Label uppercase, letter-spaced; time below it. |
| Pills | MANHÃ: teal round + pink square · MEIO-DIA: peach capsule · NOITE: blue softgel · DEITAR: teal round | Bottom of each card, centred, pill centre ≈ 62 px above the card's bottom edge. |
| Legend | `Metformina 500mg`, `Vitamina D 1000IU`, `Ómega 3 1000mg`, `Magnésio 250mg` | Two columns, filled row by row, in order of first appearance across the slots. Small pill icon before each. |

"5 doses" counts one dose per medication per slot (MANHÃ 2 + MEIO-DIA 1 + NOITE 1 + DEITAR 1).

## REPOR EM BREVE panel (`repor-em-breve.png`)

| Element | Content (exact) | Notes |
| --- | --- | --- |
| Title | `REPOR EM BREVE` | Same style as `HOJE` (y 1007–1035). |
| Title meta | `2 medicamentos` | Number of medications running low. |
| Divider | — | 1 px, white ~35 %, at y 1070, full width. |
| Jars | Ómega 3 (3 peach capsules), Metformina (6 teal round pills) | Glass jar, lid with light-blue vertical stripes (x 208–397, y 1115–1161), body to y 1478, highlight bar on the left. Pills piled from the bottom, 3 per row. |
| Name | `Ómega 3`, `Metformina` | Name only, without the strength. White, medium. |
| Count | `Restam 3`, `Restam 6` | Amber. |

## Sampled colours

| Token | Hex | Sampled at |
| --- | --- | --- |
| Teal pill | `#8EE8E2` | `hoje.png` 140,1418 |
| Pink pill | `#FE9EB0` | `hoje.png` 244,1431 |
| Peach pill | `#FCD3A2` | `hoje.png` 430,1415 |
| Blue pill | `#AAD2FC` | `hoje.png` 653,1431 |
| Lid base / stripe | `#F3F3EF` / `#D6E2F0` | `repor-em-breve.png` 226,1133 / 219,1133 |
| Amber (`Restam`) | `#F8E29F` | `repor-em-breve.png` box 200,1560,400,1610 |
| Title text | `#FDFDFB` | `hoje.png` box 95,1000,240,1050 |
| Meta text | `#D8D1CD` | `hoje.png` box 650,1000,980,1050 |

The pill colours live in `ui/.../pill/PillPalette.kt`. The amber is `RemedioPalette.stock`
(darkened to `#9A6200` on the light theme for contrast). Title, meta and surface colours now
come from the brand palette in `ui/.../theme/Theme.kt`.

## The reference data

`core/.../ReferenceFixtures.kt` reproduces both screenshots exactly: Thursday 22 October 2026,
Metformina 500mg (MANHÃ and DEITAR, 6 left), Vitamina D 1000IU (MANHÃ), Ómega 3 1000mg
(MEIO-DIA, 3 left) and Magnésio 250mg (NOITE). Screenshot tests render the panels from it.

## Rules derived for the app (not visible in the reference)

- REPOR EM BREVE lists tracked medications whose stock is at or below their threshold, fewest
  units first. The default threshold is 7 days of supply. A jar shows at most 12 pills.
- Slots always show all four cards, even when empty. A card shows at most 6 pills, then `+N`.

## Deliberate deviations

| Where | Deviation | Why |
| --- | --- | --- |
| App and widgets | Taken or skipped doses are drawn at 30 % opacity. | The panels are interactive; the reference only shows pending doses. |
| App | The author's brand background (soft blobs, light or dark) instead of the photo; frosted white or smoked cards. | Theme of mariacf.pt, requested by the owner. |
| Everywhere | Fraunces and Plus Jakarta Sans (bundled) instead of the phone's system font. | Typography of mariacf.pt; same rendering on Android and the web. |
| Pills | A thin outline in a darker shade of the pill colour. | Light pills keep their shape on white cards. |
| HOJE | The next slot with pending doses gets an accent outline. | Shows what needs attention; product addition. |
| REPOR EM BREVE | Optional blister packs (2 × 5 cells, popped cells show torn foil) instead of jars; jar glass adapts to the theme. | Product addition ("Mostrar stock como"). |
| Widgets | Letter spacing approximated with hair spaces. | Glance text has no letter spacing. |

## Product additions

Everything outside the two panels is a product addition, not reference content: the greeting
header on Hoje ("Boa tarde, *faltam 3 doses.*"), the Medicamentos list and editor, Histórico,
Definições (including the stock display and theme options and the Sobre card crediting the
author), the refill dialog, reminders and low-stock notifications, the Android widgets, and the
four extra pill colours.

## pt-PT ↔ English

| pt-PT (source) | English |
| --- | --- |
| HOJE | TODAY |
| qui., 22 · 5 doses | Thu 22 · 5 doses |
| MANHÃ · MEIO-DIA · NOITE · DEITAR | MORNING · MIDDAY · EVENING · BEDTIME |
| REPOR EM BREVE | REFILL SOON |
| 2 medicamentos | 2 medications |
| Restam 3 | 3 left |
