package org.mariacf.tempusremedium.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.schedule.Reminder
import org.mariacf.tempusremedium.core.schedule.dueDoseCount
import org.mariacf.tempusremedium.core.schedule.nextReminder
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.notification_hidden_body
import org.mariacf.tempusremedium.ui.resources.notification_slot_title
import kotlin.time.Clock

/** Re-reads the time at least this often, so the badge follows the day even without reminders. */
private const val RECHECK_MS = 15 * 60 * 1000L

/** The notification permission, as snapshot state for Definições. */
object WebNotifications {
    val supported: Boolean = notificationsSupported()

    var granted by mutableStateOf(supported && notificationPermission() == "granted")
        private set

    fun request() {
        if (supported) requestNotificationPermission { granted = it == "granted" }
    }
}

/**
 * The web's reminders, only while the page is open (a browser cannot wake a closed one): a
 * notification at each reminder time, and the number of doses due on the home screen icon.
 * Names stay out of the notification, as on a lock screen.
 */
suspend fun remindWhileOpen(repository: MedicationRepository) {
    repository.data.collectLatest { data ->
        while (true) {
            val now = currentTime()
            setAppBadge(data.dueDoseCount(now))
            val next = data.nextReminder(now)
            val wait = next?.let { millisBetween(now, it.at) } ?: RECHECK_MS
            delay(wait.coerceAtMost(RECHECK_MS))
            if (next != null && currentTime() >= next.at && WebNotifications.granted) {
                val (title, body) = data.notificationText(next)
                showNotification(title, body, tag = next.at.toString())
            }
        }
    }
}

private suspend fun AppData.notificationText(reminder: Reminder): Pair<String, String> {
    val slotDoses =
        reminder.slot?.let { slot ->
            planFor(reminder.date)
                .slots
                .firstOrNull { it.slot == slot }
                ?.doses
                ?.count { it.status == null }
        } ?: 0
    val count = slotDoses + reminder.courseDoses.size
    val what = reminder.slot?.let { getString(it.label) } ?: getString(Res.string.app_name)
    val title = getString(Res.string.notification_slot_title, what, reminder.at.time.hhmm())
    return title to getPluralString(Res.plurals.notification_hidden_body, count, count)
}

private fun currentTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun millisBetween(
    from: LocalDateTime,
    to: LocalDateTime,
): Long {
    val zone = TimeZone.currentSystemDefault()
    return (to.toInstant(zone) - from.toInstant(zone)).inWholeMilliseconds
}
