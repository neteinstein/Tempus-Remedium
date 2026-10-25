# Play Store listing

Everything the Play Console asks for to publish Tempus Remedium, in **pt-PT** (default language)
and **en-US**, plus the demo video. The folder follows the layout of Gradle Play Publisher's
`src/main/play` directory, so it can be wired into the build later (see the end of this file); for
now it is uploaded by hand.

```
store-listing/
├── default-language.txt          pt-PT
├── contact-website.txt           https://mariacf.pt
├── listings/<locale>/
│   ├── title.txt                 App name on the listing (≤ 30)
│   ├── short-description.txt     (≤ 80)
│   ├── full-description.txt      (≤ 4000), ends with the medical disclaimer Play requires
│   └── graphics/
│       ├── icon/1.png            512 × 512, 32-bit PNG
│       ├── feature-graphic/1.png 1024 × 500, PNG without alpha
│       ├── phone-screenshots/    8 × 1080 × 1920 (9:16), PNG without alpha
│       ├── tablet-screenshots/   7" tablets: 4 × 1920 × 1080 (16:9), PNG without alpha
│       └── large-tablet-screenshots/  10" tablets: 4 × 2560 × 1440 (16:9), PNG without alpha
├── release-notes/<locale>/default.txt   "What's new" for the first release (≤ 500)
├── privacy-policy/
│   ├── index.html                Bilingual, self-contained page to host
│   └── pt-PT.md, en-US.md        Same text in Markdown
├── play-console/app-content.md   Answers for App content: data safety, content rating,
│                                 target audience, health apps, ads, permissions…
├── video/                        Demo video: plan, en-US video, posters, share copy
└── scripts/                      Graphics and video pipeline, and the limits checker
```

The pt-PT video is `docs/tempus-remedium.mp4` (shown in the main README, with `docs/demo.webp` as
its preview); the en-US one is `video/tempus-remedium-en-US.mp4`.

## Before publishing

These need the owner and cannot be filled in from the repository:

1. **The dose calculator.** Courses for some antibiotic oral suspensions get a suggested dose from
   the child's weight. That may make the app a regulated medical device, which changes the Health
   apps declaration and the disclaimer in both descriptions. `play-console/app-content.md` →
   Health apps explains the options; decide before submitting.
2. **Contact email.** Play requires a public contact email. Add it in Store settings (and, if the
   build is ever wired to Gradle Play Publisher, as `contact-email.txt` here).
3. **Privacy policy URL.** Host `privacy-policy/index.html` at a public URL (a page on mariacf.pt
   is the natural place) and enter it in App content → Privacy policy.
4. **Promo video.** Play only takes a YouTube URL. Upload the two videos to YouTube (public or
   unlisted, ads off) and enter each URL in its listing.
5. **First release is a manual upload** of the `.aab` from the GitHub Release (CI_CD.md →
   Play Console). New personal developer accounts also need a 14-day closed test with 12 testers
   before production.

## Where each file goes in the Play Console

| Play Console | File |
| --- | --- |
| Grow users → Store presence → Main store listing → App name | `listings/<locale>/title.txt` |
| … → Short description | `listings/<locale>/short-description.txt` |
| … → Full description | `listings/<locale>/full-description.txt` |
| … → App icon | `listings/<locale>/graphics/icon/1.png` (same image for both) |
| … → Feature graphic | `listings/<locale>/graphics/feature-graphic/1.png` |
| … → Phone screenshots | `listings/<locale>/graphics/phone-screenshots/1.png` … `8.png`, in order |
| … → 7-inch tablet screenshots | `listings/<locale>/graphics/tablet-screenshots/1.png` … `4.png`, in order |
| … → 10-inch tablet screenshots | `listings/<locale>/graphics/large-tablet-screenshots/1.png` … `4.png`, in order |
| … → Video | YouTube URL of `docs/tempus-remedium.mp4` (pt-PT) or `video/tempus-remedium-en-US.mp4` |
| Main store listing → Manage translations → add **English (United States) – en-US** | the `en-US` files above |
| Release → Release notes | `release-notes/<locale>/default.txt` (between `<pt-PT>` / `<en-US>` tags) |
| Policy → App content, Store settings | `play-console/app-content.md` |

Not provided, all optional: Chromebook, Wear OS, TV or XR assets.

## The screenshots

Each screenshot is the real app rendered by Robolectric with sample data (the design reference's
medications, translated for en-US, with a month of history), framed on the brand gradient with a
caption in Fraunces. Captions are in `scripts/captions.json`; their order there is the order on the
listing (Play takes at most 8 phone screenshots).

| # | Screen | pt-PT caption | en-US caption |
| --- | --- | --- | --- |
| 1 | Hoje, midday | O que tomar hoje, *e a que horas.* | What to take today, *and when.* |
| 2 | Lock screen at midday: the HOJE widget under the clock | As doses de hoje, *no ecrã de bloqueio.* | Today's doses, *on your lock screen.* |
| 3 | Hoje at 13:40: MEIO-DIA in red, Ómega 3's jar pulsing | Esqueceu-se de uma toma? *Fica a vermelho.* | Missed a dose? *It turns red.* |
| 4 | Manhã sheet: one dose taken, one to take | Marque cada dose *com um toque.* | Log each dose *with one tap.* |
| 5 | Medicamentos: tablets, inhaler, patch, spray, drops, injection | Comprimidos, inaladores, *pensos, gotas e mais.* | Pills, inhalers, patches, *drops and more.* |
| 6 | Hoje with an antibiotic course every 8 hours | Antibióticos de 8 em 8 horas, *até à última toma.* | Antibiotics every 8 hours, *to the last dose.* |
| 7 | Histórico, 95 % | Os últimos 30 dias, *num relance.* | The last 30 days, *at a glance.* |
| 8 | Home screen at night: the same widgets, dark, with blister packs | Frascos ou blisters, *de dia ou de noite.* | Jars or blister packs, *day or night.* |

The captures also include Hoje in the dark theme (`07_today_dark`) and Definições
(`08_settings`); they are left out of the listing to make room for the widgets, and can be swapped
back in through `captions.json`.

The tablet screenshots (7" and 10", landscape) are 1, 4, 5 and 7 above, rendered at 960 × 600 dp
and 1280 × 800 dp, with the caption on one line. The app has no tablet layout yet, so they show the
phone layout at full width; recapture them if one is added.

Screens are rendered at 412 × 824 dp, a common phone width. Three details of the captures:

- The app works out the device language once per process, so each capture class sets its language
  in the sample settings; that is why Definições → Idioma shows the language selected.
- Tests never run infinite animations, so the running-out pulse is captured at its start (alpha
  0.15) next to the same screen with the alert seen. The glow is alpha-blended, so
  `scripts/captures.py` rebuilds any phase exactly from the pair; screenshot 3 uses the brightest.
- Robolectric cannot run a launcher or a lock screen, so they are drawn in the capture
  (wallpaper, clock, date, padlock, gesture bar) around the real Glance widgets, rendered the way
  the widget preview images are.

## The demo video

Built with the brag workflow (brag-slim): `video/brag-plan.md` has the angle and the storyboard.
24 s, 1920 × 1080, 30 fps, focused on day-to-day medication: today's doses, the reminder with
**Tomei** / **Adiar 10 min**, the HOJE widget on the lock screen and the widgets on the home
screen, and the red highlight of a missed time and of a medication running out. Every
frame is drawn from the same Robolectric captures with the app's fonts and colours; the music and
effects are synthesised in `scripts/make_video.py` at 120 BPM, with every cut on a beat. The
notification card is drawn from the strings the app's reminder uses, because Robolectric cannot
render the system shade. `video/share-copy.txt` has a caption for posting it.

## Regenerating

After a visual change to the app, a change to the sample data or a new caption:

```bash
STORE_LISTING=1 ./gradlew :androidApp:testDebugUnitTest --tests "*StoreListingCaptures*" --rerun
python3 store-listing/scripts/compose_graphics.py
python3 store-listing/scripts/make_video.py
python3 store-listing/scripts/check_listing.py
```

1. `StoreListingCaptures` (`androidApp/src/test/.../store/`) writes the raw phone and tablet
   screens, the widget lock and home screens, the feature graphic, the icon and the medication forms to
   `store-listing/source/` (git-ignored). It only
   runs with `STORE_LISTING=1`, so CI skips it; `--rerun` stops Gradle from reusing a cached result.
2. `compose_graphics.py` frames the phone and tablet screens, flattens the feature graphic and writes everything
   under `listings/<locale>/graphics/`.
3. `make_video.py` renders both videos, their posters and the README preview (needs ffmpeg).
   `--still 2.2 --locale pt-PT` renders single frames into `source/video-work/` for checking.
4. `check_listing.py` checks text lengths, image sizes, alpha and screenshot counts against the
   Play limits.

The scripts need Pillow and NumPy (`pip install pillow numpy`). The feature graphic's tagline is
in `StoreListingCaptures`; the icon is the launcher icon's two layers, cropped to what a launcher
shows.

## Keeping it true

- If a feature, a user-visible string or a permission changes, update both full descriptions, the
  privacy policy (all three files) and `play-console/app-content.md`.
- The "no data collected" answer and the privacy policy rely on the app having no `INTERNET`
  permission. Adding one means redoing the Data safety form and the policy before that release.

## Wiring into Gradle Play Publisher (later)

Gradle Play Publisher reads this layout from `androidApp/src/main/play/`. Moving or symlinking the
listing files there would let `./gradlew publishListing` upload the text and graphics, and would
make `publishReleaseBundle` (run by `release.yml`) attach `release-notes/` to every release, so
the notes would then need updating per release. Keep `source/`, `scripts/`, `privacy-policy/`,
`play-console/` and `video/` out of that directory if you do.
