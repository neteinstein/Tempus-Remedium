package org.mariacf.tempusremedium.android.reminders

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.CourseFixtures
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.LowStockItem
import org.mariacf.tempusremedium.core.schedule.logCourseDosesAt
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT")
class ReminderNotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
    private val notifications = ReminderNotifications(context)

    @Before
    fun grant() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun plain(data: AppData) = data.copy(settings = data.settings.copy(alarmReminders = false))

    private fun posted(slot: TimeSlot): Notification = manager.getNotification(ReminderNotifications.slotNotificationId(slot))

    @Test
    fun slotReminderListsPendingDosesWithActions() =
        runBlocking {
            notifications.showSlot(plain(ReferenceFixtures.data), ReferenceFixtures.date, TimeSlot.MORNING)

            val notification = posted(TimeSlot.MORNING)
            assertEquals("Manhã · 08:00", notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("Metformina 500mg, Vitamina D 1000IU", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(listOf("Tomei", "Adiar 10 min"), notification.actions.map { it.title.toString() })
            assertEquals(NotificationCompat.VISIBILITY_PUBLIC, notification.visibility)
        }

    @Test
    fun plainRemindersDoNotRing() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(alarmReminders = false))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)

            val notification = posted(TimeSlot.MORNING)
            assertEquals(ReminderNotifications.CHANNEL_REMINDERS, notification.channelId)
            assertFalse(notification.flags and Notification.FLAG_INSISTENT != 0)
            assertEquals(Notification.CATEGORY_REMINDER, notification.category)
            assertTrue(rung().isEmpty())
        }

    /** The reminders handed to [AlarmRingService], in order, with the sound to ring. */
    private fun rung(): List<Pair<Notification, String?>> =
        generateSequence { shadowOf(context).nextStartedService }
            .filter { it.component?.className == AlarmRingService::class.java.name }
            .map { IntentCompat.getParcelableExtra(it, "notification", Notification::class.java)!! to it.getStringExtra("sound") }
            .toList()

    @Test
    fun alarmRemindersRingOnTheAlarmStreamPastSilentMode() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(alarmReminders = true))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)
            notifications.showCourseDoses(
                CourseFixtures.data.copy(settings = Settings(alarmReminders = true)),
                LocalDateTime(ReferenceFixtures.date, LocalTime(16, 0)),
            )

            val rung = rung()
            assertEquals(
                listOf("Manhã · 08:00", "Amoxicilina 250 mg/5 mL · 16:00"),
                rung.map { it.first.extras.getString(Notification.EXTRA_TITLE) },
            )
            for ((notification, sound) in rung) {
                assertEquals(ReminderNotifications.CHANNEL_REMINDERS_RINGING, notification.channelId)
                assertEquals(Notification.CATEGORY_ALARM, notification.category)
                assertNotNull(notification.deleteIntent)
                assertNull(sound)
            }
            // The service rings; the channel's own sound would follow the ringer mode.
            assertTrue(manager.allNotifications.isEmpty())
            val channel =
                context
                    .getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(ReminderNotifications.CHANNEL_REMINDERS_RINGING)
            assertNull(channel.sound)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        }

    @Test
    fun remindersThatFollowThePhonesModeAreNotAlarms() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(alarmReminders = true, alarmOverridesSilence = false))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)

            val started = shadowOf(context).nextStartedService
            assertFalse(started.getBooleanExtra("override_silence", true))
            val notification = IntentCompat.getParcelableExtra(started, "notification", Notification::class.java)!!
            assertEquals(Notification.CATEGORY_REMINDER, notification.category)
        }

    @Test
    fun alarmIsTheDefault() = assertTrue(Settings().alarmReminders)

    @Test
    fun aChosenSoundRingsAndGetsItsOwnFallbackChannel() =
        runBlocking {
            val sound = "content://media/internal/audio/media/42"
            val system = context.getSystemService(NotificationManager::class.java)
            notifications.ensureChannels(Settings())
            notifications.showSlot(
                ReferenceFixtures.data.copy(settings = Settings(alarmSound = sound)),
                ReferenceFixtures.date,
                TimeSlot.MORNING,
            )

            assertEquals(sound, rung().single().second)
            val channelId = ReminderNotifications.alarmChannelId(sound)
            assertEquals(sound, system.getNotificationChannel(channelId).sound.toString())
            assertEquals(AudioAttributes.USAGE_ALARM, system.getNotificationChannel(channelId).audioAttributes.usage)
            val alarmChannels =
                system.notificationChannels.map { it.id }.filter {
                    it.startsWith(
                        ReminderNotifications.CHANNEL_REMINDERS_ALARM,
                    )
                }
            assertEquals(listOf(channelId), alarmChannels)
        }

    @Test
    fun hiddenNamesKeepTheLockScreenVersionGeneric() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(hideNamesInReminders = true, alarmReminders = false))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)

            val notification = posted(TimeSlot.MORNING)
            assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
            assertEquals(
                "2 doses para tomar",
                notification.publicVersion.extras
                    .getCharSequence(Notification.EXTRA_TEXT)
                    .toString(),
            )
        }

    @Test
    fun nothingIsPostedWhenRemindersAreOff() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(remindersEnabled = false))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)
            assertTrue(manager.allNotifications.isEmpty())
        }

    @Test
    fun courseReminderShowsTheDoseAndItsNumber() =
        runBlocking {
            notifications.showCourseDoses(plain(CourseFixtures.data), LocalDateTime(ReferenceFixtures.date, LocalTime(16, 0)))

            val notification = manager.getNotification(ReminderNotifications.COURSE_NOTIFICATION_ID)
            assertEquals("Amoxicilina 250 mg/5 mL · 16:00", notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("6 mL · toma 2 de 21", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(listOf("Tomei", "Adiar 10 min"), notification.actions.map { it.title.toString() })
        }

    @Test
    fun noCourseReminderWhenTheDoseIsLogged() =
        runBlocking {
            val at = LocalDateTime(ReferenceFixtures.date, LocalTime(16, 0))
            notifications.showCourseDoses(CourseFixtures.data.logCourseDosesAt(at, DoseStatus.TAKEN, 1L), at)
            assertTrue(manager.allNotifications.isEmpty())
        }

    @Test
    fun lowStockWarningUsesTheReferenceWording() =
        runBlocking {
            notifications.showLowStock(LowStockItem(ReferenceFixtures.omega3, 3))
            val notification = manager.allNotifications.single()
            assertEquals("Repor em breve", notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("Ómega 3 · Restam 3", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(ReminderNotifications.CHANNEL_LOW_STOCK, notification.channelId)
            assertTrue(
                context.getSystemService(NotificationManager::class.java).getNotificationChannel(ReminderNotifications.CHANNEL_LOW_STOCK) !=
                    null,
            )
        }
}
