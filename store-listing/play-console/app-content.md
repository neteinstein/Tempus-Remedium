# Play Console answers

What to enter in the Play Console forms that are not part of the store listing text. These forms are
answered once per app (not per language); where a form takes free text, the text is given in pt-PT
and English.

The answers describe the app as built on 25 October 2026. Re-check them whenever the merged manifest
gains a permission or a dependency starts sending data. In particular, the "no data collected"
answer rests on the app having **no `INTERNET` permission**.

Merged release manifest permissions at the time of writing (`processReleaseMainManifest`):
`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` (from the app);
`WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` (from WorkManager, used by Glance);
`…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX, internal). No `INTERNET`, no `AD_ID`, no
`FOREGROUND_SERVICE_*` type permissions.

## Create app

| Field | Answer |
| --- | --- |
| App name | `Tempus Remedium` (the localised listing titles add the descriptor, see `listings/*/title.txt`) |
| Default language | Portuguese (Portugal) – pt-PT |
| App or game | App |
| Free or paid | Free |
| Declarations | Accept the Developer Program Policies and US export laws (account owner) |

## Store settings

| Field | Answer |
| --- | --- |
| Category | **Medical** (medication schedules and adherence; the closest Play category) |
| Tags | Up to 5 from Play's list, closest to: medication reminder, pill tracker, health records, reminders |
| Contact email | **To fill in**: a public address the author is happy to show on the listing (required) |
| Website | `https://mariacf.pt` |
| Phone | Leave empty |
| External marketing | Leave on (default) |

## App content (Policy → App content)

### Privacy policy

Host `privacy-policy/index.html` at a public, non-geofenced URL (not a PDF) and enter that URL, for
example a page on mariacf.pt. Play also shows it on the listing.

### App access

**All functionality is available without special access.** There is no login, account or
subscription.

### Ads

**No, my app does not contain ads.**

### Content rating (IARC questionnaire)

| Question | Answer |
| --- | --- |
| Email for the rating certificate | Same contact email as above |
| Category | **All other app types** (utility, productivity, communication or other) |
| Violence, fear, sexuality, crude humour, gambling | No |
| Language (profanity) | No |
| Controlled substances (illegal drugs, alcohol, tobacco) | No. The app records the user's own prescribed or over-the-counter medication; it does not depict or promote drugs, alcohol or tobacco. |
| Users can interact or exchange content | No |
| Shares the user's current physical location with other users | No |
| Allows users to purchase digital goods | No |
| Unrestricted internet access (web browser, search engine) | No |
| Miscellaneous (e.g. Nazi symbolism, horror) | No |

Expected result: PEGI 3, ESRB Everyone, USK 0, ClassInd L, and equivalents.

### Target audience and content

| Question | Answer |
| --- | --- |
| Target age groups | **18 and over** only |
| Could the app unintentionally appeal to children? | No |
| Store presence | Not in the Families / Teacher Approved programmes |

Choosing 18+ keeps the app out of the Families policy requirements; it is a medication manager for
adults. The paediatric dose calculator does not change this: it is used by the parents or carers
who give the medication, not by children.

### News apps

**No**, it is not a news app.

### COVID-19 contact tracing and status apps

**My app is not a publicly available COVID-19 contact tracing or status app.**

### Data safety

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |

That ends the form; the listing then shows "No data collected" and "No data shared with third
parties". Why this is accurate (keep for the reviewer and for future changes):

- Google defines "collect" as transmitting data off the device. The app has no `INTERNET`
  permission, so it cannot transmit anything; data stays in the app's private storage.
- Android Auto Backup (`android:allowBackup="true"`) is a system backup to the user's own Google
  Account under Google's terms; the developer never receives it. The privacy policy mentions it.
- No SDK for analytics, crash reporting, ads or sign-in is included.

If a future version adds `INTERNET` (crash reporting, sync, anything), this form and the privacy
policy must be redone before release, and the "health info" data type will almost certainly apply.

### Government apps

**No**, the app is not developed by or on behalf of a government.

### Financial features

**My app doesn't provide any financial features.**

### Health apps

| Section | Answer |
| --- | --- |
| Health features | **Medical → Medication and Treatment Management** (medication schedules, reminders, courses and adherence) and **Medical → Clinical Decision Support** (the dose calculator, see below) |
| Medical device | **Decision for the author, see below.** The answer prepared here is "not regulated as a medical device", which is what the listing's disclaimer says. |
| Health Connect / sensitive health permissions | None used |
| Health subjects research | No |

**The dose calculator needs a decision before publishing.** When a course is created for
amoxicillin, amoxicillin + clavulanic acid, azithromycin or cefuroxime oral suspension, the app
suggests a dose in mL, an interval and a length from the child's weight (`core/.../dosing/DoseCalculator.kt`,
ported from mariacf.pt/calculadora-doses). Everything else in the app records and reminds, but
software that works out a patient-specific dose is the kind of function regulators may treat as a
medical device (in the EU, under the MDR's rules for medical device software). If it is one, Play
requires declaring it and showing proof of clearance, and the "not a medical device" sentence in
both descriptions must go. The options are to confirm with a regulatory adviser that it is not one,
to obtain the clearance, or to leave the calculator out of the Play build. Google's form lists drug
dosage calculators under Clinical Decision Support, which is why that category is ticked above.

Policy requirement met by the listing: both full descriptions say the app is not a medical device,
does not diagnose, treat, cure or prevent any condition, say the calculator's suggestions do not
replace a prescription, and tell users to consult a healthcare professional. The privacy policy
covers the handling of health-related data, including that the child's weight is not stored.

### Advertising ID

**No**, the app does not use an advertising ID (no `com.google.android.gms.permission.AD_ID`).

### Sensitive permissions and APIs

- **Exact alarms.** The app uses `SCHEDULE_EXACT_ALARM` (granted by the user under *Alarms &
  reminders*), not the restricted `USE_EXACT_ALARM`. Without it, reminders fall back to an inexact
  window. If the Play Console asks for a justification, use:

  - pt-PT: *O Tempus Remedium é um gestor de medicação. A função principal é lembrar a pessoa de
    tomar cada dose à hora que ela definiu (Manhã, Meio-dia, Noite e Deitar). O alarme exato
    garante que o lembrete chega a essa hora, mesmo com o dispositivo em repouso. A permissão é
    pedida ao utilizador e, se for recusada, os lembretes continuam a funcionar com alguns minutos
    de atraso.*
  - English: *Tempus Remedium is a medication manager. Its core function is to remind the user to
    take each dose at the time they set (Morning, Midday, Evening and Bedtime). An exact alarm makes
    sure the reminder arrives at that time, even when the device is idle. The user grants the
    permission; if they refuse it, reminders still work but may arrive a few minutes late.*

- **Foreground services.** None of the app's own; WorkManager's `SystemForegroundService` is
  declared without a type and no `FOREGROUND_SERVICE_*` permission is present, so no foreground
  service declaration is needed.

## Release

- **First upload is manual** (see CI_CD.md → Play Console): download the `.aab` from the GitHub
  Release and upload it to the internal testing track. Later releases go through
  `publishReleaseBundle`.
- Release name: the version name, e.g. `1.0.42`.
- Release notes: `release-notes/pt-PT/default.txt` and `release-notes/en-US/default.txt`.
- Countries: start with Portugal; add others as wanted (the listing exists in pt-PT and en-US).
- New personal developer accounts must run a closed test with at least 12 testers for 14 days
  before applying for production access.
