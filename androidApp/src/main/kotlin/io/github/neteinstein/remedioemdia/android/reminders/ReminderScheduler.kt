package io.github.neteinstein.remedioemdia.android.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.nextReminder
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps exactly one alarm for the next slot with pending doses, plus a daily
 * alarm just after midnight so the widgets move to the new day.
 */
class ReminderScheduler(
    private val context: Context,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun schedule(data: AppData) {
        val zone = timeZone()
        val now = clock.now().toLocalDateTime(zone)
        val next = data.nextReminder(now)
        if (next == null) {
            alarms.cancel(ReminderReceiver.showIntent(context, REQUEST_NEXT, null, null))
        } else {
            setAlarm(
                next.at.toInstant(zone).toEpochMilliseconds(),
                ReminderReceiver.showIntent(context, REQUEST_NEXT, next.date, next.slot),
            )
        }
        val tomorrow = now.date.plus(DatePeriod(days = 1))
        alarms.set(
            AlarmManager.RTC,
            tomorrow.atStartOfDayIn(zone).toEpochMilliseconds() + MIDNIGHT_DELAY_MS,
            ReminderReceiver.dayChangedIntent(context),
        )
    }

    fun snooze(
        date: LocalDate,
        slot: TimeSlot,
    ) {
        val at = clock.now() + SNOOZE
        setAlarm(at.toEpochMilliseconds(), ReminderReceiver.showIntent(context, REQUEST_SNOOZE, date, slot))
    }

    private fun setAlarm(
        atMillis: Long,
        intent: PendingIntent,
    ) {
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, atMillis, WINDOW_MS, intent)
        }
    }

    companion object {
        const val REQUEST_NEXT = 1
        const val REQUEST_SNOOZE = 2
        val SNOOZE = 10.minutes
        private const val WINDOW_MS = 10 * 60 * 1000L
        private const val MIDNIGHT_DELAY_MS = 30 * 1000L

        /** Epoch millis of a slot on a date, for tests and logging. */
        fun millisOf(
            date: LocalDate,
            time: LocalTime,
            zone: TimeZone,
        ): Long = LocalDateTime(date, time).toInstant(zone).toEpochMilliseconds()
    }
}
