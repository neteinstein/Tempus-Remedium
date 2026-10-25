# AGENTS.md

Guidance for coding agents (and humans) working on **Tempus Remedium**, a medication and dose
manager in European Portuguese by **Dra. Maria Cortês Ferreira** ([mariacf.pt](https://mariacf.pt)).
Android is the primary platform; the web build is a reduced version of the same app.
`CLAUDE.md` imports this file.

## Project map

Kotlin Multiplatform + Compose Multiplatform. Gradle modules:

| Module | Targets | What lives there |
| --- | --- | --- |
| `core` | JVM, Wasm | Model (`Medication`, `Schedule`, `DoseLog`, `AppData`), the day plan, stock, adherence, reminder maths, the JSON repository and storage (`FileDocumentStorage` on JVM/Android, `LocalStorageDocumentStorage` on the web), `ReferenceFixtures`. Pure Kotlin, fully unit-tested. |
| `ui` | Android, Wasm | Theme (`TempusTheme`, light and dark `TempusPalette`), pill, jar and blister painters, the HOJE and REPOR EM BREVE panels, all screens, `App`, `AppViewModel`, string resources (`composeResources/values` = pt-PT, `values-en`, `values-fr`, `values-de`, `values-es` = translations), the Fraunces and Plus Jakarta Sans fonts. |
| `androidApp` | Android | `MainActivity`, `AppContainer`, reminders (`AlarmManager`, notification actions, reschedule on boot/time change), Glance widgets, Android-only settings, Robolectric and Roborazzi tests. |
| `webApp` | Wasm | `main()` with `ComposeViewport`, `index.html`; the installable web app (`manifest.webmanifest`, `sw.js` with the `precache.json` the build writes, icons), the install bar (iOS: add to home screen; Android: Google Play), notifications and icon badge while the page is open (`WebReminders.kt`). |

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
  frosted surfaces on a near-white ground with soft blobs; a dark variant. `TempusPalette.Light` and
  `TempusPalette.Dark` mirror the CSS custom properties of `https://mariacf.pt/assets/css/styles.css`
  (`--bg`, `--ink*`, `--surface*`, `--teal` → `accent`, `--blob-*`, `--brand-*`). Re-read that file
  before changing tokens; it was reachable from the agent container.
- Pill colours, the amber of "Restam N" and the panel layouts stay those of the design reference.
- Never use `TempusPalette`/fonts directly in composables: read `TempusTheme.colors` and
  `TempusTheme.type`. Widgets draw bitmaps, so they pick a palette with `WidgetStyle.palette(context)`.

## Commands

Each line mirrors a job of `.github/workflows/pr.yml` (see [`CI_CD.md`](CI_CD.md)):

```bash
./gradlew ktlintCheck                                      # style gate (ktlint + Compose rules); ktlintFormat fixes   [ktlint]
./gradlew :androidApp:lintDebug                            # Android Lint                                            [ktlint]
./gradlew assembleDebug                                    # compile                                                 [Compile]
./gradlew assembleRelease && ./scripts/verify-obfuscation.sh   # minified/obfuscated release + R8 mapping checks     [Minified Release]
./gradlew :core:jvmTest :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug   # unit + screenshot tests [Unit Tests]
./gradlew :webApp:wasmJsBrowserDistribution                # web build → webApp/build/dist/wasmJs/productionExecutable [Compile Web]
./gradlew iosSimulatorArm64Test :ui:compileKotlinIosSimulatorArm64   # iOS (simulator tests need macOS)             [Compile iOS]
./gradlew :core:jvmTest :androidApp:testDebugUnitTest koverXmlReport koverVerify :core:koverVerify   # coverage    [Code Coverage]
```

Day to day:

```bash
./gradlew :androidApp:recordRoborazziDebug                 # re-record screenshot goldens after a visual change
./gradlew :webApp:wasmJsBrowserDevelopmentRun              # web dev server
./gradlew :core:jvmTest --tests "*.DayPlanTest"            # one test class
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
  English, French, German and Spanish (`values-en`, `-fr`, `-de`, `-es`) are translations of them;
  add every new string to all five. Definições → Idioma overrides the device language
  (`LocalAppLocale` in `ui`). Never hard-code user-visible text.
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

What *can* be checked offline once the SDK is installed: every PR Checks job except the iOS
simulator tests. That is `ktlintCheck`, `:androidApp:lintDebug`, `assembleDebug`, `assembleRelease`
with `verify-obfuscation.sh`, `:core:jvmTest`, `:androidApp:testDebugUnitTest` (Robolectric,
including notification, alarm and widget-content tests), Roborazzi screenshots, Kover, the Wasm
build, the iOS *compilation* (`:core:compileKotlinIosSimulatorArm64 :ui:compileKotlinIosSimulatorArm64`;
Kotlin/Native cross-compiles Apple klibs on Linux), and the web app itself (serve the dist folder with `python3 -m http.server` and drive it with the
preinstalled Playwright Chromium).

**CI on the pull request is the real build signal.** Say in the PR's test plan what you could not run.

## Conventions

- **Conventional Commits** for commits and PR titles: `feat(android): …`, `fix(core): …`,
  `docs(design): …`, `ci: …`, `test(ui): …`. Scopes: `core`, `ui`, `android`, `web`, `design`, `ci`.
  CI checks the PR title.
- Fill in the PR template (`.github/pull_request_template.md`): Summary, Changes, Test plan (including
  what could not be run and why), Risk.
- Kotlin style: ktlint official style + Compose rules through the ktlint Gradle plugin, applied to
  every project from the root `build.gradle.kts` (`.editorconfig` holds the settings). Run
  `./gradlew ktlintFormat` before committing; generated sources are excluded.
- Releases, signing secrets, Play publishing and R8 rules: [`CI_CD.md`](CI_CD.md). Only add a
  `-keep` to `androidApp/proguard-rules.pro` for something resolved by name at runtime, and extend
  `scripts/verify-obfuscation.sh` when you do.
- New logic goes in `core` with tests; UI changes come with re-recorded screenshots.
- CI/CD topology, secrets and signing are documented in [`CI_CD.md`](CI_CD.md), not here.
