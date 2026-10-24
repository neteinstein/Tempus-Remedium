# Remédio em Dia

Medication and dose manager, in European Portuguese. It shows what to take today and when
(**HOJE**), which medications are running out (**REPOR EM BREVE**), and keeps a history of the
doses taken. Android first, plus a reduced web version.

<p>
  <img src="androidApp/src/test/screenshots/screen_today.png" width="260" alt="Hoje and Repor em breve">
  <img src="androidApp/src/test/screenshots/screen_medications.png" width="260" alt="Medicamentos">
  <img src="androidApp/src/test/screenshots/screen_history.png" width="260" alt="Histórico">
</p>

## Features

| | Android | Web |
| --- | :---: | :---: |
| HOJE: doses by MANHÃ / MEIO-DIA / NOITE / DEITAR, mark as taken or skipped | ✅ | ✅ |
| REPOR EM BREVE: stock tracking, low-stock list, refill | ✅ | ✅ |
| Medications: name, strength, pill shape and colour, slots, frequency, stock | ✅ | ✅ |
| History: 30-day adherence | ✅ | ✅ |
| Configurable slot times | ✅ | ✅ |
| Reminder notifications with **Tomei** / **Adiar 10 min**, shown on the lock screen (names can be hidden) | ✅ | — |
| Low-stock notifications | ✅ | — |
| Home-screen widgets: Hoje and Repor em breve (also offered for the lock screen where the system supports it) | ✅ | — |
| Data storage | On the device | In the browser (localStorage) |

pt-PT by default; English when the device or browser language is English. No account and no
server: data never leaves the device or browser.

## Build

Requires JDK 21 and, for Android, the Android SDK (`scripts/setup-android-sdk.sh` installs one).

```bash
./gradlew :androidApp:assembleDebug            # Android APK
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # web app on a local dev server
./gradlew :core:jvmTest :androidApp:testDebugUnitTest
```

More commands and conventions: [AGENTS.md](AGENTS.md). Pipelines: [CI_CD.md](CI_CD.md).
Design source of truth: [docs/design/REFERENCE.md](docs/design/REFERENCE.md).

## Licences

Inter font © The Inter Project Authors, SIL Open Font License 1.1 ([licenses/Inter-OFL.txt](licenses/Inter-OFL.txt)).
