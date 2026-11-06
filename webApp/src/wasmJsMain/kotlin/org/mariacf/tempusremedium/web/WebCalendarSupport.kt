package org.mariacf.tempusremedium.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.IcsEvent
import org.mariacf.tempusremedium.core.schedule.calendarExportLastDay
import org.mariacf.tempusremedium.core.schedule.calendarReminders
import org.mariacf.tempusremedium.core.schedule.icsCalendar
import org.mariacf.tempusremedium.core.schedule.markCalendarExported
import org.mariacf.tempusremedium.ui.CalendarSupport
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
 * The calendar export of Definições and of the iPhone's bar. A page cannot tell whether the file
 * reached the calendar, so the export is only recorded, to spot later changes, on confirm.
 */
class WebCalendarSupport(
    private val repository: MedicationRepository,
    override val offers: Boolean,
) : CalendarSupport {
    private val scope = MainScope()
    private var exportedAt by mutableStateOf<LocalDateTime?>(null)

    override val awaitingConfirmation: Boolean
        get() = exportedAt != null

    override fun add() {
        scope.launch { exportedAt = exportCalendar(repository.data.value) }
    }

    override fun confirm() {
        val at = exportedAt ?: return
        exportedAt = null
        scope.launch { repository.update { it.markCalendarExported(at) } }
    }

    override fun cancel() {
        exportedAt = null
    }
}

/**
 * The reminders of the coming days as a calendar file with an alert per event, for reminders
 * with the page closed. Returns the time of the export, for markCalendarExported. Names stay out, as in the web's notifications: calendars sync and show
 * on lock screens.
 */
private suspend fun exportCalendar(data: AppData): LocalDateTime {
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
    // A download, not the share sheet: Safari opens a calendar file with "Add All" to Calendar.
    downloadFile("tempus-remedium.ics", "text/calendar", icsCalendar(appName, events + again, instant))
    return now
}
