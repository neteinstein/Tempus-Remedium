# CI/CD

## Workflows

| Workflow | File | Trigger |
| --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Every pull request; pushes to `main` |
| PR title | `.github/workflows/pr-title.yml` | Pull request opened, edited or updated |
| Deploy web | `.github/workflows/deploy-web.yml` | Pushes to `main`; manual run |

### CI jobs (run in parallel)

| Job | Command | Artifacts |
| --- | --- | --- |
| `build-android` | `./gradlew :androidApp:assembleDebug` | `remedio-em-dia-debug-apk` |
| `build-web` | `./gradlew :webApp:wasmJsBrowserDistribution` | `remedio-em-dia-web` (static site) |
| `unit-tests` | `./gradlew :core:jvmTest :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug` | `test-reports` (JUnit, Roborazzi diffs) |
| `lint` | `./gradlew spotlessCheck :androidApp:lintDebug` | `lint-reports` |
| `coverage` | `./gradlew koverXmlReport koverHtmlReport koverVerify :core:koverVerify` | `coverage-report`; line coverage in the job summary |
| iOS | — | Not applicable: there is no iOS target yet (see below). |

All jobs use `ubuntu-latest`, Temurin JDK 21 and `gradle/actions/setup-gradle` for caching. The
runner's preinstalled Android SDK is used. Superseded runs on the same branch are cancelled.

Coverage thresholds (Kover): `core` ≥ 85 % lines, merged core + Android ≥ 70 % lines.
Screenshot goldens live in `androidApp/src/test/screenshots/`. A visual change fails `unit-tests`
until the goldens are re-recorded with `./gradlew :androidApp:recordRoborazziDebug` and committed.

### PR title

Rejects titles that are not Conventional Commits (`type(scope): subject`, with type one of feat, fix,
docs, style, refactor, perf, test, build, ci, chore or revert).

### Deploy web

Builds the Wasm distribution and publishes it to GitHub Pages.
**One-time setup:** in *Settings → Pages*, set *Source* to **GitHub Actions**. Until then the
workflow fails at the deploy step. It only runs on `main`, so pull requests are unaffected.

## Secrets

None are needed today. All workflows use the default `GITHUB_TOKEN` with least-privilege permissions.

To ship signed Android releases later, add these as repository secrets and a release job that
decodes the keystore before `assembleRelease`/`bundleRelease`:

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Upload keystore, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

## Signing and distribution per platform

| Platform | Status |
| --- | --- |
| Android | **Debug-signed only.** CI builds a debug APK (signed with the runner's debug key) as an artifact. No release build, no Play upload. |
| Web | No signing needed. Static Wasm site deployed to GitHub Pages from `main`. |
| iOS | **Not applicable.** No iOS target exists. |

## Adding iOS later

Compose Multiplatform supports iOS. Add `iosArm64()` and `iosSimulatorArm64()` to `core` and `ui`,
create an Xcode host app, then add a `build-ios` job on `macos-latest` that runs
`./gradlew :core:iosSimulatorArm64Test :ui:compileKotlinIosSimulatorArm64`. Signing then needs an
Apple certificate and a provisioning profile as secrets.

## Running CI locally

The same Gradle commands work locally. In sandboxed agent containers, first run
`scripts/setup-android-sdk.sh --maven-mirror` (see AGENTS.md).
