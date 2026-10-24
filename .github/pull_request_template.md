## Summary

<!-- What does this change do, and why? One or two sentences. -->

## Changes

<!-- Bullet list of the notable changes, grouped by module (core, ui, androidApp, webApp, CI, docs). -->

-

## Test plan

<!-- What you ran and what it showed. Then, explicitly, what you could NOT run and why
     (e.g. "no emulator in the agent container: widgets and notifications not checked on a device"). -->

- [ ] `./gradlew ktlintCheck :androidApp:lintDebug`
- [ ] `./gradlew :core:jvmTest :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug`
- [ ] `./gradlew assembleDebug :webApp:wasmJsBrowserDistribution`
- [ ] Release-affecting changes: `./gradlew assembleRelease && ./scripts/verify-obfuscation.sh`
- [ ] UI changes: screenshots re-recorded, checked in light **and** dark theme, compared with `docs/design/reference` (`scripts/compare-reference.py`)
- [ ] No hardcoded user-visible strings (all in `composeResources`), no secrets or keys committed

**Not run, and why:**

## Risk

<!-- What could break (data format, reminders, widgets, permissions)? How would we notice and roll back? -->
