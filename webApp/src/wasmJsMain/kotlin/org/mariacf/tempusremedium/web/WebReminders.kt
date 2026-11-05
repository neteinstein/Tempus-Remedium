package org.mariacf.tempusremedium.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import org.mariacf.tempusremedium.core.schedule.newlyLowStock
import org.mariacf.tempusremedium.core.schedule.nextReminder
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.notification_hidden_body
import org.mariacf.tempusremedium.ui.resources.notification_low_stock_hidden
import org.mariacf.tempusremedium.ui.resources.notification_slot_title
import org.mariacf.tempusremedium.ui.resources.refill_soon
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** Re-reads the time at least this often, so the badge follows the day even without reminders. */
private const val RECHECK_MS = 15 * 60 * 1000L

/** The notification permission, as snapshot state for Definições. */
object WebNotifications {
    val supported: Boolean = notificationsSupported()

    var granted by mutableStateOf(readGranted())
        private set

    fun request() {
        if (supported) requestNotificationPermission { granted = it == "granted" }
    }

    /** The permission may have changed in the phone's settings while the app was away. */
    fun refresh() {
        granted = readGranted()
    }

    private fun readGranted() = supported && notificationPermission() == "granted"
}

/** Reminders that fell due further back than this while the page slept are not worth a notification. */
private const val MISSED_WINDOW_MINUTES = 60L

/**
 * The web's reminders, only while the page is open (a browser cannot wake a closed one): a
 * notification at each reminder time, the number of doses due on the home screen icon, and a
 * notification when a medication runs low. Names stay out of the notifications, as on a lock
 * screen. iOS suspends a page in the background, so coming back to the front re-checks at once
 * and still announces a slot that fell due meanwhile.
 */
suspend fun remindWhileOpen(repository: MedicationRepository) {
    val wake = Channel<Unit>(Channel.CONFLATED)
    onPageVisible {
        WebNotifications.refresh()
        wake.trySend(Unit)
    }
    coroutineScope {
        launch { warnLowStock(repository) }
        var checkedUpTo = currentTime()
        repository.data.collectLatest { data ->
            while (true) {
                val now = currentTime()
                setAppBadge(data.dueDoseCount(now))
                val due = data.latestReminderBetween(maxOf(checkedUpTo, now.minusMinutes(MISSED_WINDOW_MINUTES)), now)
                checkedUpTo = now
                if (due != null && WebNotifications.granted) {
                    val (title, body) = data.notificationText(due)
                    showNotification(title, body, tag = due.at.toString(), url = due.url())
                }
                val next = data.nextReminder(now)
                val wait = next?.let { millisBetween(now, it.at) } ?: RECHECK_MS
                withTimeoutOrNull(wait.coerceIn(0, RECHECK_MS)) { wake.receive() }
            }
        }
    }
}

/** The last reminder strictly after [from] and no later than [to], if any. */
private fun AppData.latestReminderBetween(
    from: LocalDateTime,
    to: LocalDateTime,
): Reminder? = generateSequence(nextReminder(from)) { nextReminder(it.at) }.takeWhile { it.at <= to }.lastOrNull()

/** Opens the slot's sheet when the notification is tapped (see main). */
private fun Reminder.url(): String = slot?.let { "./?slot=${it.name}" } ?: "./"

/** "Repor em breve" once per medication that runs low, as on Android, without its name. */
private suspend fun warnLowStock(repository: MedicationRepository) {
    var previous: AppData? = null
    repository.data.collect { data ->
        val before = previous
        previous = data
        if (before == null || !WebNotifications.granted) return@collect
        data.newlyLowStock(before).forEach { item ->
            showNotification(
                title = getString(Res.string.refill_soon),
                body = getString(Res.string.notification_low_stock_hidden),
                tag = "low-stock-${item.medication.id}",
                url = "./?refill=${item.medication.id}",
            )
        }
    }
}

private fun LocalDateTime.minusMinutes(minutes: Long): LocalDateTime {
    val zone = TimeZone.currentSystemDefault()
    return (toInstant(zone) - minutes.minutes).toLocalDateTime(zone)
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

internal fun currentTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun millisBetween(
    from: LocalDateTime,
    to: LocalDateTime,
): Long {
    val zone = TimeZone.currentSystemDefault()
    return (to.toInstant(zone) - from.toInstant(zone)).inWholeMilliseconds
}
