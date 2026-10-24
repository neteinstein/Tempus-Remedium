# CI/CD

The pipeline follows the project owner's standard Android + KMP pipeline: PR checks in parallel
jobs, signed releases from `main` to GitHub Releases and the Play Console, and the web build on
GitHub Pages. Where this project deviates from that standard, the last section says why.

## Workflows

| Workflow | File | Trigger |
| --- | --- | --- |
| PR Checks | `.github/workflows/pr.yml` | Pull requests into `main` or `develop` (opened, synchronised, reopened) |
| Release | `.github/workflows/release.yml` | Pushes to `main`; manual run |
| Deploy Web (GitHub Pages) | `.github/workflows/deploy-pages.yml` | Pushes to `main`; manual run |
| PR title | `.github/workflows/pr-title.yml` | Pull requests (opened, edited, updated) |
| Dependabot | `.github/dependabot.yml` | Weekly `gradle` and `github-actions` update PRs |

All workflows use Temurin JDK 21, `actions/setup-java` with the Gradle cache, `gradle/actions/setup-gradle`,
and run Gradle with `--no-daemon --stacktrace`. PR Checks cancels superseded runs of the same PR.

### PR Checks (parallel jobs)

| Job | Runner | Command | Artifacts |
| --- | --- | --- | --- |
| `ktlint` | ubuntu | `./gradlew ktlintCheck`, then `./gradlew :androidApp:lintDebug` | `lint-reports` |
| `Compile` | ubuntu | `./gradlew assembleDebug` | `debug-apk` |
| `Minified Release` | ubuntu | `./gradlew assembleRelease` + `./scripts/verify-obfuscation.sh` | `r8-mapping-files` |
| `Unit Tests` | ubuntu | `./gradlew :core:jvmTest :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug` | `unit-test-results`; results posted on the PR as "🧪 Unit Test Results" |
| `Compile Web` | ubuntu | `./gradlew :webApp:wasmJsBrowserDistribution` | — |
| `Compile iOS` | **macos** | `./gradlew iosSimulatorArm64Test :ui:compileKotlinIosSimulatorArm64` | — |
| `Code Coverage` | ubuntu | `./gradlew :core:jvmTest :androidApp:testDebugUnitTest koverXmlReport koverHtmlReport koverVerify :core:koverVerify` | `coverage-reports`; uploaded to Codecov |

- **Minified Release** catches broken R8 keep rules before they reach `main` (`assembleDebug`
  never runs R8). The script checks that app classes were renamed, that the manifest components
  kept their names and that the kotlinx.serialization serializers for the stored data survived.
- **Unit Tests** compares the Roborazzi screenshots with the goldens in
  `androidApp/src/test/screenshots/`. A visual change fails until they are re-recorded with
  `./gradlew :androidApp:recordRoborazziDebug` and committed.
- **Compile iOS**: there is no iOS app yet. `core` and `ui` target `iosArm64`/`iosSimulatorArm64`
  so shared code keeps compiling for iOS. `core`'s `commonTest` runs on the simulator.
- **Code Coverage** thresholds (Kover): `core` ≥ 85 % lines, merged core + Android ≥ 70 %.
  Codecov upload never fails the job; set `CODECOV_TOKEN` once the repo is added on codecov.io.

### Release

On every push to `main`:

1. Fails fast unless the four signing secrets exist.
2. Runs `ktlintCheck` and the unit tests again on the exact commit being released (a squash or
   rebase merge lands a commit PR Checks never built).
3. Decodes the keystore and builds the signed `assembleRelease` APK and `bundleRelease` App Bundle,
   with `versionCode` = run number and `versionName` = `1.0.<run number>`.
4. Runs `scripts/verify-obfuscation.sh` again.
5. Creates the GitHub Release `v1.0.<run number>` with `TempusRemedium_version1_0_<n>.apk`, the
   `.aab` and the R8 `-mapping.txt`, SHA-1s in the notes, plus generated notes.
6. If `ANDROID_PUBLISHER_CREDENTIALS` is set, uploads the bundle to the Play Console with
   `publishReleaseBundle` (Gradle Play Publisher) to the `internal` track, or `PLAY_TRACK`.
   Otherwise it logs a notice and skips.

### Deploy Web (GitHub Pages)

Builds the Wasm distribution and publishes it with `actions/deploy-pages` to
https://neteinstein.github.io/Tempus-Remedium/.
**One-time setup:** *Settings → Pages → Build and deployment → Source* = **GitHub Actions**.

### PR title

Rejects titles that are not Conventional Commits (`type(scope): subject`, with type one of feat,
fix, docs, style, refactor, perf, test, build, ci, chore or revert).

## Secrets and variables

| Name | Kind | Used by | Purpose |
| --- | --- | --- | --- |
| `KEYSTORE_BASE64` | secret | Release | Upload keystore (`.jks`/`.keystore`), base64-encoded: `base64 -w0 release.keystore` |
| `KEYSTORE_PASSWORD` | secret | Release | Keystore password |
| `KEY_ALIAS` | secret | Release | Key alias |
| `KEY_PASSWORD` | secret | Release | Key password |
| `ANDROID_PUBLISHER_CREDENTIALS` | secret, optional | Release | Raw JSON of a Play Console service account key with release permission for this app |
| `PLAY_TRACK` | variable, optional | Release | Play track to publish to (default `internal`) |
| `CODECOV_TOKEN` | secret, optional | PR Checks | Codecov upload token |

Until the four signing secrets are set, **Release fails on every push to `main`** at its first
step. PR Checks need no secrets: the minified build falls back to debug signing.

`androidApp/build.gradle.kts` reads `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
`APP_VERSION_CODE`, `APP_VERSION_NAME` and `PLAY_TRACK` from the environment the workflow sets,
falling back to debug signing and `1` / `1.0.0` locally.

## Signing and distribution per platform

| Platform | Status |
| --- | --- |
| Android | Release builds are minified (R8 full mode), resource-shrunk and signed with the upload key from the secrets. Published on GitHub Releases (APK + AAB + mapping) and, once configured, to the Play Console's internal track. PRs build a debug APK and a debug-signed minified release. |
| Web | No signing. Static Wasm site deployed to GitHub Pages from `main`. |
| iOS | Compile and simulator tests only (no app, no signing). |

## Play Console

The Play Developer API can only update an app that already has one release, so the **first
upload is manual**: download the `.aab` from the GitHub Release and upload it under Play Console →
the app → Testing or Production → Create release. Every release after that is automated by
`publishReleaseBundle`. The Play listing will also need a privacy policy URL.

**Troubleshooting a `403 PERMISSION_DENIED` from `publishReleaseBundle`:** the failing call is the
first `POST .../applications/org.mariacf.tempusremedium/edits`, so the cause is the
service account's standing, not this repo. Check, in order: (1) the service account (`client_email`
in the JSON) is invited under Play Console → Users and permissions with access to this app and
release permission; (2) a first release was uploaded manually; (3) the Google Play Android Developer
API is enabled on the key's Google Cloud project; (4) new permissions can take a few hours to
propagate, so re-run later.

## Obfuscation and shrinking

`release` is minified, obfuscated and resource-shrunk (`isMinifyEnabled`, `isShrinkResources`,
`android.enableR8.fullMode=true`). `androidApp/proguard-rules.pro` keeps almost nothing: only add
a `-keep` for something reached **by name at runtime**, with a comment saying what resolves it.
Manifest components are kept by AGP; kotlinx.serialization, Glance, WorkManager and coroutines ship
their own rules. Traces keep line numbers (`-keepattributes SourceFile,LineNumberTable`) and are
decoded with the release's `mapping.txt` (`retrace mapping.txt trace.txt`).

## Running the checks locally

The same Gradle commands work locally; see AGENTS.md → Commands. In sandboxed agent containers,
first run `scripts/setup-android-sdk.sh --maven-mirror`. `Compile iOS` needs macOS for the simulator
tests; on Linux `./gradlew :core:compileKotlinIosSimulatorArm64 :ui:compileKotlinIosSimulatorArm64`
still checks that the iOS code compiles.

## Deviations from the standard pipeline

| Standard | Here | Why |
| --- | --- | --- |
| JDK 17 | JDK 21 | Robolectric's SDK 36 jars (screenshot and notification tests) need Java 21. |
| `github` and `playstore` flavors | One release variant | There is no in-app self-update to strip for the Play build, so both flavors would be identical. Hence `testDebugUnitTest`, `publishReleaseBundle`, one APK. |
| AGP coverage for androidApp + Kover for KMP modules | Kover for both | Kover handles the Android application module too; one report, one tool. |
| `verify-obfuscation.sh` checks Room's `_Impl` | Checks manifest components and the serializers of the stored JSON | No Room here; those are what this app resolves by name. |
| ktlint job runs ktlint only | Also runs Android Lint | Kept from this project's earlier pipeline. |
| No PR title check | `pr-title.yml` | Enforces Conventional Commits. |
| Unit tests | Plus Roborazzi screenshot verification | Guards the design reference. |
