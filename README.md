<div align="center">

<img src="docs/icon.svg" width="128" height="128" alt="Ícone do Tempus Remedium">

# Tempus Remedium

**Os seus medicamentos e doses de hoje, em dia.**

**Por [Dra. Maria Cortês Ferreira](https://mariacf.pt)**, médica de família · [mariacf.pt/apps](https://mariacf.pt/apps/)

[![PR Checks](https://github.com/neteinstein/Tempus-Remedium/actions/workflows/pr.yml/badge.svg)](https://github.com/neteinstein/Tempus-Remedium/actions/workflows/pr.yml)
![Android 8+](https://img.shields.io/badge/Android-8%2B-3DDC84?logo=android&logoColor=white)
![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)
![Languages](https://img.shields.io/badge/idiomas-PT%20%C2%B7%20EN%20%C2%B7%20FR%20%C2%B7%20DE%20%C2%B7%20ES-026A9F)
![No account, no server](https://img.shields.io/badge/sem%20conta-sem%20servidor-14B8A6)

<a href="https://github.com/neteinstein/Tempus-Remedium/raw/main/docs/tempus-remedium.mp4">
  <img src="docs/demo.webp" width="860" alt="Demonstração do Tempus Remedium: as tomas do dia, o lembrete com Tomei e Adiar 10 min, os widgets no ecrã de bloqueio e no ecrã principal, o horário em atraso que fica a vermelho e o frasco que pisca quando está a acabar">
</a>

<sub>▶ <a href="https://github.com/neteinstein/Tempus-Remedium/raw/main/docs/tempus-remedium.mp4">Ver com som</a> · 24 segundos</sub>

</div>

---

Medication and dose manager, in European Portuguese. It shows what to take today and when
(**HOJE**), which medications are running out (**REPOR EM BREVE**, as jars or blister packs),
and keeps a history of the doses taken. Android first, plus a reduced web version (not deployed
for now, see [CI_CD.md](CI_CD.md#deploy-web)). The look
follows the author's site, [mariacf.pt](https://mariacf.pt): Fraunces and Plus Jakarta Sans,
calm blues, frosted cards, and a light and a dark theme.

<p>
  <img src="androidApp/src/test/screenshots/screen_today.png" width="250" alt="Hoje (claro)">
  <img src="androidApp/src/test/screenshots/screen_today_dark.png" width="250" alt="Hoje (escuro)">
  <img src="androidApp/src/test/screenshots/screen_today_blisters.png" width="250" alt="Repor em breve com blisters">
</p>
<p>
  <img src="androidApp/src/test/screenshots/screen_medications.png" width="250" alt="Medicamentos">
  <img src="androidApp/src/test/screenshots/screen_history.png" width="250" alt="Histórico">
  <img src="androidApp/src/test/screenshots/screen_settings_dark.png" width="250" alt="Definições">
</p>

**Forms and recurrence**

<p>
  <img src="androidApp/src/test/screenshots/screen_medications_recurrences.png" width="250" alt="Medicamentos: penso transdérmico a cada 3 dias, inalador de 2 em 2 semanas, spray nasal até uma data, injetável mensal e gotas em ciclo">
  <img src="androidApp/src/test/screenshots/pill_forms.png" width="250" alt="Todas as formas em todas as cores: comprimidos, cápsulas, inalador, penso, spray nasal, gotas e injetável">
  <img src="androidApp/src/test/screenshots/screen_editor_patch.png" width="250" alt="Editor: penso transdérmico a cada 3 dias, a partir de hoje, 10 vezes">
</p>

**Widgets, alerts and reminders**

<p>
  <img src="androidApp/src/test/screenshots/screen_today_running_out.png" width="250" alt="Hoje: horários em atraso a vermelho e o frasco do Ómega 3 a piscar porque acaba amanhã">
  <img src="androidApp/src/test/screenshots/screen_widget_guide.png" width="250" alt="Barra para adicionar o widget e o guia passo a passo">
  <img src="androidApp/src/test/screenshots/screen_settings_permissions.png" width="250" alt="Definições: secção Permissões com as permissões em falta a vermelho">
</p>
<p>
  <img src="docs/images/phone_home_widgets.png" width="280" alt="Ecrã principal com os widgets Hoje, Tratamento e Repor em breve">
  <img src="docs/images/phone_lock_widget.png" width="280" alt="Ecrã de bloqueio com o widget Hoje">
</p>
<p>
  <img src="androidApp/src/main/res/drawable-nodpi/widget_hoje_preview.webp" width="250" alt="Widget Hoje">
  <img src="androidApp/src/main/res/drawable-night-nodpi/widget_repor_preview.webp" width="250" alt="Widget Repor em breve (escuro)">
  <img src="androidApp/src/main/res/drawable-nodpi/widget_course_preview.webp" width="250" alt="Widget Tratamento">
</p>
<p>
  <img src="docs/images/web_today.png" width="380" alt="Versão web (claro)">
  <img src="docs/images/web_today_dark.png" width="380" alt="Versão web (escuro)">
</p>

## Features

| | Android | Web |
| --- | :---: | :---: |
| HOJE: doses by MANHÃ / MEIO-DIA / NOITE / DEITAR, mark as taken or skipped; slots past their time with doses still pending turn **red** | ✅ | ✅ |
| REPOR EM BREVE: stock tracking, low-stock list, refill, shown as **jars or blister packs**; a jar or blister that runs out today or tomorrow **pulses red** until tapped | ✅ | ✅ |
| Medications: name, strength, form (pills, capsules, inhaler, patch, nasal spray, drops, injection) and colour, slots, recurrence (daily, weekdays every N weeks, every N days, monthly, on/off cycles; start date; ends never, on a date or after N times), stock | ✅ | ✅ |
| Courses (e.g. antibiotics): a dose every N hours, ending after N days, N doses, on a date or when the pack runs out; dose, interval and length suggested by the author's [dose calculator](https://mariacf.pt/calculadora-doses/) | ✅ | ✅ |
| History: 30-day adherence | ✅ | ✅ |
| Configurable slot times; automatic, light or dark theme | ✅ | ✅ |
| Reminder notifications with **Tomei** / **Adiar 10 min**, shown on the lock screen (names can be hidden) | ✅ | Only while the app is open, without names or buttons (Definições → Avisos com a app aberta) |
| Reminders in the phone's calendar: the next 90 days as events with an alert, without names, that ring with the app closed (Definições → Adicionar ao calendário; a snapshot to add again after changes) | — | ✅ |
| Number of doses due on the app icon | — | ✅ Installed web app, where the system supports it |
| Low-stock notifications | ✅ | — |
| Home-screen widgets: Hoje, Repor em breve and Tratamento (also offered for the lock screen where the system supports it), with previews in the widget picker | ✅ | — |
| "Add the widget" bar and a step-by-step guide (home and lock screen), with one-tap pinning where the launcher allows it | ✅ | — |
| Permissões section in Definições: a missing reminder permission (notifications, on-time alarms) shows in red, with a red bar on top that leads there | ✅ | — |
| Installable on the home screen, works offline (manifest and service worker) | — | ✅ |
| Install bar in a phone's browser: on iPhone/iPad, how to add the app to the home screen; on Android, a link to the native app on Google Play | — | ✅ |
| Data storage | On the device | In the browser (localStorage) |

Português (pt-PT), English, Français, Deutsch and Español: the device or browser language by default, or the one chosen in Definições → Idioma. No account and no
server: data never leaves the device or browser. The app supports care and does not replace
advice from a doctor or pharmacist.

## Build

Requires JDK 21 and, for Android, the Android SDK (`scripts/setup-android-sdk.sh` installs one).

```bash
./gradlew assembleDebug                        # Android APK
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # web app on a local dev server
./gradlew :core:jvmTest :androidApp:testDebugUnitTest
```

Releases are built and published by CI on every push to `main` (GitHub Releases, and the Play
Console once configured); see [CI_CD.md](CI_CD.md).

More commands and conventions: [AGENTS.md](AGENTS.md). Pipelines: [CI_CD.md](CI_CD.md).
Design source of truth: [docs/design/REFERENCE.md](docs/design/REFERENCE.md).

## Author

**Dra. Maria Cortês Ferreira**, family doctor. More of her work: [mariacf.pt](https://mariacf.pt).

## Licences

Fonts, both under the SIL Open Font License 1.1:
Fraunces © The Fraunces Project Authors ([licenses/Fraunces-OFL.txt](licenses/Fraunces-OFL.txt)) and
Plus Jakarta Sans © The Plus Jakarta Sans Project Authors ([licenses/PlusJakartaSans-OFL.txt](licenses/PlusJakartaSans-OFL.txt)).
