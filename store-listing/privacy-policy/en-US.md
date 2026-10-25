# Tempus Remedium privacy policy

Last updated: 25 October 2026

Tempus Remedium is an Android app for managing medications and doses, created by
Dr Maria Cortês Ferreira, family doctor ([mariacf.pt](https://mariacf.pt)). This policy explains
what data the app uses and where it is kept.

## Summary

- The app does not collect or share personal data.
- The app has no permission to access the internet: it sends no data to any server, the
  author's or anyone else's.
- There is no account, no advertising, no usage analytics and no tracking.

## What the app stores

The data you enter: your medications (name, strength, shape, colour, times, frequency, stock and
notes), your courses (dose, interval, first dose and end), the record of doses taken or skipped,
and your settings (times, appearance, language and reminders). This data can reveal information
about your health. The child's weight entered in the dose calculator is only used for the
calculation and is not stored.

It is stored only in the app's private storage on your device. Other apps cannot read it and the
author has no access to it. Because the app never sends any data off the device, the author does
not process your personal data. Links to mariacf.pt open in the device's browser.

## Notifications and widgets

Reminders, low-stock alerts and widgets are produced on the device itself. In Settings you can
hide medication names on the lock screen.

## Android backups

If Android backup is turned on, the system may include the app's data in your device backup,
stored in your Google Account under Google's terms, or transfer it to a new device. That backup is
handled by Android and Google; the author has no access to it. You can turn it off in Android's
settings.

## Permissions

| Permission | Why |
| --- | --- |
| Notifications (`POST_NOTIFICATIONS`) | Show dose reminders and low-stock alerts. |
| Alarms & reminders (`SCHEDULE_EXACT_ALARM`) | Show reminders on time. Without it they may arrive a few minutes late. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Schedule reminders again after the device restarts or the time changes. |
| `WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` | Requested by the Android library (WorkManager) the widgets rely on. The app does not use the network: `ACCESS_NETWORK_STATE` only tells whether a connection exists and cannot send data. |

## Deleting your data

You can delete a medication and its history in the app. To delete everything, clear the app's
storage in Android's settings or uninstall the app.

## Children

The app is meant for adults and is not directed at children.

## Changes to this policy

If this policy changes, the new version is published at this same address with an updated date.

## Contact

Dr Maria Cortês Ferreira · [mariacf.pt](https://mariacf.pt)
