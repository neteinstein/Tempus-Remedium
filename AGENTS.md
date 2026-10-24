# AGENTS.md

Guidance for coding agents (and humans) working on **Remédio em Dia**, a medication and dose
manager in European Portuguese by **Dra. Maria Cortês Ferreira** ([mariacf.pt](https://mariacf.pt)).
Android is the primary platform; the web build is a reduced version of the same app.
`CLAUDE.md` imports this file.

## Project map

Kotlin Multiplatform + Compose Multiplatform. Gradle modules:

| Module | Targets | What lives there |
| --- | --- | --- |
| `core` | JVM, Wasm | Model (`Medication`, `Schedule`, `DoseLog`, `AppData`), the day plan, stock, adherence, reminder maths, the JSON repository and storage (`FileDocumentStorage` on JVM/Android, `LocalStorageDocumentStorage` on the web), `ReferenceFixtures`. Pure Kotlin, fully unit-tested. |
| `ui` | Android, Wasm | Theme (`RemedioTheme`, light and dark `RemedioPalette`), pill, jar and blister painters, the HOJE and REPOR EM BREVE panels, all screens, `App`, `AppViewModel`, string resources (`composeResources/values` = pt-PT, `values-en` = English), the Fraunces and Plus Jakarta Sans fonts. |
| `androidApp` | Android | `MainActivity`, `AppContainer`, reminders (`AlarmManager`, notification actions, reschedule on boot/time change), Glance widgets, Android-only settings, Robolectric and Roborazzi tests. |
| `webApp` | Wasm | `main()` with `ComposeViewport`, `index.html`. |

Key ideas:

- All data is one `AppData` JSON document. Every change is a **pure function** in
  `core/.../schedule/Mutations.kt` applied through `MedicationRepository.update`. Stock follows the
  logs: taking a dose uses stock, undoing gives it back.
- `AppContainer` (Android) observes the repository and keeps the next reminder alarm, the widgets
  and low-stock notifications in sync, so callers only change data.
- Widgets cannot run Compose drawing code. They draw pills and jars with the shared painters into
  bitmaps (`PillBitmaps`).

## Author and visual identity

- The author is **Dra. Maria Cortês Ferreira**, family doctor. She is credited in Definições → Sobre
  (with a link to https://mariacf.pt), on the web page (`<meta name="author">`, splash) and in the README.
  Keep the credit wherever the app presents itself.
- The theme follows her site, **mariacf.pt**: Fraunces (display, with an italic accent in the
  headline) and Plus Jakarta Sans (body); calm blues with the brand gradient `#026A9F → #125A98 → #00668B`;
  frosted surfaces on a near-white ground with soft blobs; a dark variant. `RemedioPalette.Light` and
  `RemedioPalette.Dark` mirror the CSS custom properties of `https://mariacf.pt/assets/css/styles.css`
  (`--bg`, `--ink*`, `--surface*`, `--teal` → `accent`, `--blob-*`, `--brand-*`). Re-read that file
  before changing tokens; it was reachable from the agent container.
- Pill colours, the amber of "Restam N" and the panel layouts stay those of the design reference.
- Never use `RemedioPalette`/fonts directly in composables: read `RemedioTheme.colors` and
  `RemedioTheme.type`. Widgets draw bitmaps, so they pick a palette with `WidgetStyle.palette(context)`.

## Commands

```bash
./gradlew :core:jvmTest                                    # domain tests (fast, no Android SDK needed)
./gradlew :androidApp:testDebugUnitTest                    # Robolectric tests (notifications, alarms, widgets)
./gradlew :androidApp:recordRoborazziDebug                 # re-record screenshot goldens
./gradlew :androidApp:verifyRoborazziDebug                 # compare screenshots with the goldens (CI)
./gradlew spotlessApply                                    # format (ktlint + Compose rules)
./gradlew spotlessCheck :androidApp:lintDebug              # lint (CI)
./gradlew koverXmlReport koverVerify :core:koverVerify     # coverage + thresholds (core ≥ 85 %, merged ≥ 70 %)
./gradlew :androidApp:assembleDebug                        # APK
./gradlew :webApp:wasmJsBrowserDistribution                # web build → webApp/build/dist/wasmJs/productionExecutable
./gradlew :webApp:wasmJsBrowserDevelopmentRun              # web dev server
```

JDK 21. The Android SDK path goes in `local.properties` (`scripts/setup-android-sdk.sh` writes it).

## Source of truth for content and design

- `docs/design/reference/*.png` and their transcription `docs/design/REFERENCE.md` define the
  structure and content of the HOJE and REPOR EM BREVE panels (colours and type follow the author's
  brand, see above). **Copy strings exactly** (accents, case, `·`, spacing), keep the
  order, and **do not "clean up" or invent content** that is not in the source: no rewording
  (`Restam` stays `Restam`), no extra badges or copy inside the panels. Deliberate deviations are
  listed in REFERENCE.md; add to that list instead of silently diverging.
- The screenshots were supplied directly by the project owner; there is no origin URL to re-fetch.
  They contain phone and screenshot-viewer chrome (clock, buttons, counters) that is not part of the
  design; REFERENCE.md lists it.
- pt-PT strings (`ui/src/commonMain/composeResources/values/strings.xml`) are authoritative;
  English (`values-en`) is a translation of them. Never hard-code user-visible text.
- `ReferenceFixtures` reproduces the reference data and must keep doing so.
- Do not reference other apps or products in code, docs, commits or PRs.

Recurring derivation work is packaged as a skill: **`.claude/skills/match-reference-design`**
(render a component, compare with the reference image, iterate). Use it rather than re-deriving
measurements.

## Cloud / sandboxed agent containers

Agent containers may not be able to build everything. What was true when this was written:

- **No Android SDK preinstalled.** `dl.google.com` was reachable, so `scripts/setup-android-sdk.sh`
  installs cmdline-tools, platform 37 and build-tools 37 into `~/android-sdk` and writes
  `local.properties`. If that host is blocked, only `:core` builds.
- **Maven Central is flaky through the egress proxy** (intermittent 404/429 that Gradle caches).
  Run `scripts/setup-android-sdk.sh --maven-mirror` to install
  `scripts/sandbox/maven-mirror.init.gradle.kts` into `~/.gradle/init.d`. It adds Google's Maven
  Central mirror and points Robolectric's downloader at it. Do not commit mirror settings into the build.
- **github.com release downloads are blocked (403).** That is why `gradle.properties` sets
  `kotlin.js.yarn=false` (npm instead of yarn) and why the fonts were fetched from Google Fonts
  (`fonts.googleapis.com` / `fonts.gstatic.com`).
- **No emulator** (`/dev/kvm` is absent). Instrumented tests, real notifications, exact-alarm
  timing and widgets on a launcher **cannot be checked** in a container.

What *can* be checked offline once the SDK is installed: `spotlessCheck`, `:core:jvmTest`,
`:androidApp:testDebugUnitTest` (Robolectric, including notification, alarm and widget-content
tests), Roborazzi screenshots, `:androidApp:lintDebug`, `:androidApp:assembleDebug`, the Wasm build,
and the web app itself (serve the dist folder with `python3 -m http.server` and drive it with the
preinstalled Playwright Chromium).

**CI on the pull request is the real build signal.** Say in the PR's test plan what you could not run.

## Conventions

- **Conventional Commits** for commits and PR titles: `feat(android): …`, `fix(core): …`,
  `docs(design): …`, `ci: …`, `test(ui): …`. Scopes: `core`, `ui`, `android`, `web`, `design`, `ci`.
  CI checks the PR title.
- Fill in the PR template (`.github/pull_request_template.md`): Summary, Changes, Test plan (including
  what could not be run and why), Risk.
- Kotlin style: ktlint official style + Compose rules through Spotless (`.editorconfig`). Run
  `./gradlew spotlessApply` before committing.
- New logic goes in `core` with tests; UI changes come with re-recorded screenshots.
- CI/CD topology, secrets and signing are documented in [`CI_CD.md`](CI_CD.md), not here.
