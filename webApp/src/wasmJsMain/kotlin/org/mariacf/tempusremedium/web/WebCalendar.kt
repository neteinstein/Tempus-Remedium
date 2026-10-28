package org.mariacf.tempusremedium.web

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.IcsEvent
import org.mariacf.tempusremedium.core.schedule.calendarExportLastDay
import org.mariacf.tempusremedium.core.schedule.calendarReminders
import org.mariacf.tempusremedium.core.schedule.icsCalendar
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.calendar_export_again
import org.mariacf.tempusremedium.ui.resources.calendar_export_again_body
import org.mariacf.tempusremedium.ui.resources.notification_hidden_body
import org.mariacf.tempusremedium.ui.resources.notification_slot_title
import kotlin.time.Clock

private const val UID_DOMAIN = "tempusremedium.mariacf.pt"

/**
 * The reminders of the coming days as a calendar file with an alert per event, for reminders
 * with the page closed. Names stay out, as in the web's notifications: calendars sync and show
 * on lock screens.
 */
suspend fun exportCalendar(data: AppData) {
    val instant = Clock.System.now()
    val now = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val appName = getString(Res.string.app_name)
    val events =
        data.calendarReminders(now).map { reminder ->
            val what = reminder.slot?.let { getString(it.label) } ?: appName
            IcsEvent(
                uid = "${reminder.at}-${reminder.slot?.name ?: "course"}@$UID_DOMAIN",
                start = reminder.at,
                summary = getString(Res.string.notification_slot_title, what, reminder.at.time.hhmm()),
                description = getPluralString(Res.plurals.notification_hidden_body, reminder.doseCount, reminder.doseCount),
            )
        }
    val lastDay = calendarExportLastDay(now.date)
    val again =
        IcsEvent(
            uid = "$lastDay-export-again@$UID_DOMAIN",
            start = LocalDateTime(lastDay, data.settings.timeOf(TimeSlot.MORNING)),
            summary = getString(Res.string.calendar_export_again),
            description = getString(Res.string.calendar_export_again_body),
        )
    downloadFile("tempus-remedium.ics", "text/calendar", icsCalendar(appName, events + again, instant))
}
