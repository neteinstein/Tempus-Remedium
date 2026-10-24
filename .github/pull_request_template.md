## Summary

<!-- What does this change do, and why? One or two sentences. -->

## Changes

<!-- Bullet list of the notable changes, grouped by module (core, ui, androidApp, webApp, CI, docs). -->

-

## Test plan

<!-- What you ran and what it showed. Then, explicitly, what you could NOT run and why
     (e.g. "no emulator in the agent container: widgets and notifications not checked on a device"). -->

- [ ] `./gradlew spotlessCheck :androidApp:lintDebug`
- [ ] `./gradlew :core:jvmTest :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug`
- [ ] `./gradlew :androidApp:assembleDebug :webApp:wasmJsBrowserDistribution`
- [ ] UI changes: screenshots re-recorded and compared with `docs/design/reference` (`scripts/compare-reference.py`)

**Not run, and why:**

## Risk

<!-- What could break (data format, reminders, widgets, permissions)? How would we notice and roll back? -->
