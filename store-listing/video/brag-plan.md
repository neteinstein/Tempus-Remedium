# Demo video plan

Made with the brag workflow (brag-slim): the whole video is drawn from the app's real UI, rendered by
Robolectric (`StoreListingCaptures`), with music and effects synthesised to one beat grid.
`store-listing/scripts/make_video.py` builds it; this file is its plan.

## The app in one sentence

A medication manager by a family doctor that shows what to take today and when, reminds you at
each time, keeps the day on your lock and home screens, turns a missed time red and warns you
before a medication runs out.

- **For:** people who take several medications a day, and the people who look after them.
- **Sets it apart:** every medication is drawn as it is (shape, colour, form: tablets, inhalers,
  patches, drops…); a missed time turns red instead of hiding; no account, no internet.
- **Visual hook:** the real HOJE panel with MEIO-DIA up next.
- **Tone:** `polished`, leaning `app-store`: calm, clean, health-appropriate; soft slides and fades.
- **Share caption:** see `share-copy.txt`.

## Focus (requested)

Day-to-day medication: what to take today, the reminder at each time with **Tomei**, the widgets
on the lock screen and the home screen, and the red highlight when a dose is missed or a
medication is running out. Courses (antibiotics) and the dose calculator are left out, and so is
the medication forms scene of the previous cut.

## Format

1920 × 1080, 30 fps, 24 s. Music at **120 BPM** (one beat = 0.5 s, one bar = 2 s), key of D major,
progression D – Bm – G – A, one chord per bar; the drums stop and the final chord rings at 22 s.
Every scene change falls on a beat.

## Storyboard (pt-PT; en-US uses the app's English strings and translated copy)

| # | Time | Beats | Scene | On screen | Sound |
| --- | --- | --- | --- | --- | --- |
| 1 | 0.0–2.5 s | 1–5 | **Hook** | "Várias tomas por dia?" in Fraunces, left. Right: the real HOJE panel at 12:40, large, MEIO-DIA up next; slow push-in. | Pad and plucks only |
| 2 | 2.5–5.5 s | 6–11 | **Reveal** | Eyebrow "TEMPUS REMEDIUM". "O que tomar hoje, *e a que horas.*" Phone slides in with Hoje at 12:40. | Drums enter on beat 6 |
| 3 | 5.5–10.5 s | 12–21 | **Reminder** | Eyebrow "LEMBRETES". "Avisa à hora certa. *Tomei, num toque.*" Phone at 07:59; the MANHÃ notification ("Manhã · 08:00", "Metformina 500mg, Vitamina D 1000IU", Tomei / Adiar 10 min) drops in on beat 13; a tap on "Tomei" on beat 17; the notification leaves and MANHÃ's pills fade as taken. | Two-note chime, soft tap |
| 4 | 10.5–16.5 s | 22–33 | **Widgets** | Eyebrow "WIDGETS". "No ecrã de bloqueio, *sem abrir a app.*" Close-up of a lock screen at 12:40 with the HOJE widget under the clock. On beat 28 the phone unlocks: the home screen rises with HOJE and REPOR EM BREVE, "E no ecrã principal, *o que falta repor.*" | A rising glassy pair on each screen |
| 5 | 16.5–20.5 s | 34–41 | **Missed, and running low** | Eyebrow "EM ATRASO". "Passou a hora? *Fica a vermelho.*" Phone at 13:40, zoomed on HOJE: MEIO-DIA in red. On beat 38 the camera moves to REPOR EM BREVE: "Está a acabar? *Avisa antes de faltar.*" while Ómega 3's jar pulses red (the app's own 700 ms pulse). | Low marimba note on the red, soft pulse ticks |
| 6 | 20.5–24.0 s | 42–48 | **Outro** | "Tempus Remedium", "por Dra. Maria Cortês Ferreira · Médica de família", the five languages, "mariacf.pt/apps". | Resolve on D, shimmer, fade |

Readability: every line stays settled for at least 0.3 s per word after it is fully in.

## Real material used

- Screens: `video_0759`, `video_0801`, `01_today`, `02_overdue` and `02_overdue_base` (the jar's pulse
  is rebuilt exactly from that pair, since the glow is alpha-blended).
- Widgets: `09_lock_screen` (HOJE on the lock screen) and `video_home` (HOJE and REPOR EM BREVE on a
  home screen, without the course widget). The widgets are the real Glance layouts; the lock and
  home screens around them are drawn in the capture.
- The notification card is drawn in the video from the strings the app's `ReminderNotifications`
  posts (title, the pending names, and the two actions); Robolectric cannot render the system shade.
- Fonts: the app's Fraunces and Plus Jakarta Sans. Colours: the app's palette.
- Sample medications are the design reference's (Metformina, Vitamina D, Ómega 3, Magnésio); no
  personal data.

## Deliverables

- `docs/tempus-remedium.mp4` (pt-PT, shown in the README) and `docs/demo.webp` (its silent preview).
- `store-listing/video/tempus-remedium-en-US.mp4` (English listing).
- `store-listing/video/poster-<locale>.jpg` (also frame 0 of each video) and `share-copy.txt`.
