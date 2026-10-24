package io.github.neteinstein.remedioemdia.android.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = TimeZone.of("Europe/Lisbon")
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val clock =
        object : Clock {
            override fun now(): Instant = ReferenceFixtures.now.toInstant(zone)
        }
    private val scheduler = ReminderScheduler(context, clock) { zone }

    private fun reminderAlarm() =
        alarms.scheduledAlarms.firstOrNull { alarm ->
            shadowOf(alarm.operation).savedIntent.action == ReminderReceiver.ACTION_SHOW
        }

    @Test
    fun schedulesTheNextSlotWithPendingDoses() {
        scheduler.schedule(ReferenceFixtures.data)

        val alarm = reminderAlarm()!!
        assertEquals(ReminderScheduler.millisOf(ReferenceFixtures.date, LocalTime(19, 0), zone), alarm.triggerAtMs)
        val intent = shadowOf(alarm.operation).savedIntent
        assertEquals(TimeSlot.EVENING.name, intent.getStringExtra(ReminderReceiver.EXTRA_SLOT))
        assertEquals("2026-10-22", intent.getStringExtra(ReminderReceiver.EXTRA_DATE))
    }

    @Test
    fun cancelsTheReminderWhenNothingIsDue() {
        scheduler.schedule(ReferenceFixtures.data)
        scheduler.schedule(AppData())
        assertNull(reminderAlarm())
    }

    @Test
    fun alwaysSchedulesTheMidnightRefresh() {
        scheduler.schedule(AppData())
        val midnight =
            alarms.scheduledAlarms.single { shadowOf(it.operation).savedIntent.action == ReminderReceiver.ACTION_DAY_CHANGED }
        assertEquals(
            ReminderScheduler.millisOf(
                ReferenceFixtures.date.let {
                    kotlinx.datetime.LocalDate(2026, 10, 23)
                },
                LocalTime(0, 0, 30),
                zone,
            ),
            midnight.triggerAtMs,
        )
    }

    @Test
    fun snoozeFiresTenMinutesLater() {
        scheduler.snooze(ReferenceFixtures.date, TimeSlot.MORNING)
        val snooze =
            alarms.scheduledAlarms.single {
                shadowOf(it.operation).savedIntent.getStringExtra(ReminderReceiver.EXTRA_SLOT) == TimeSlot.MORNING.name
            }
        assertEquals(clock.now().toEpochMilliseconds() + 10 * 60 * 1000, snooze.triggerAtMs)
    }
}
