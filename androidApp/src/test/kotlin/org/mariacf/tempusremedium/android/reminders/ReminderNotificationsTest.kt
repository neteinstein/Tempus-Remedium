package org.mariacf.tempusremedium.android.reminders

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.CourseFixtures
import org.mariacf.tempusremedium.core.ReferenceFixtures
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

    private fun posted(slot: TimeSlot): Notification = manager.getNotification(ReminderNotifications.slotNotificationId(slot))

    @Test
    fun slotReminderListsPendingDosesWithActions() =
        runBlocking {
            notifications.showSlot(ReferenceFixtures.data, ReferenceFixtures.date, TimeSlot.MORNING)

            val notification = posted(TimeSlot.MORNING)
            assertEquals("Manhã · 08:00", notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("Metformina 500mg, Vitamina D 1000IU", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(listOf("Tomei", "Adiar 10 min"), notification.actions.map { it.title.toString() })
            assertEquals(NotificationCompat.VISIBILITY_PUBLIC, notification.visibility)
        }

    @Test
    fun plainRemindersDoNotRepeatTheSound() =
        runBlocking {
            notifications.showSlot(ReferenceFixtures.data, ReferenceFixtures.date, TimeSlot.MORNING)

            val notification = posted(TimeSlot.MORNING)
            assertEquals(ReminderNotifications.CHANNEL_REMINDERS, notification.channelId)
            assertFalse(notification.flags and Notification.FLAG_INSISTENT != 0)
        }

    @Test
    fun alarmRemindersRingWithTheAlarmSoundUntilHandled() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(alarmReminders = true))
            notifications.showSlot(data, ReferenceFixtures.date, TimeSlot.MORNING)
            notifications.showCourseDoses(
                CourseFixtures.data.copy(settings = Settings(alarmReminders = true)),
                LocalDateTime(ReferenceFixtures.date, LocalTime(16, 0)),
            )

            for (notification in listOf(posted(TimeSlot.MORNING), manager.getNotification(ReminderNotifications.COURSE_NOTIFICATION_ID))) {
                assertEquals(ReminderNotifications.CHANNEL_REMINDERS_ALARM, notification.channelId)
                assertTrue(notification.flags and Notification.FLAG_INSISTENT != 0)
            }
            val channel =
                context
                    .getSystemService(
                        NotificationManager::class.java,
                    ).getNotificationChannel(ReminderNotifications.CHANNEL_REMINDERS_ALARM)
            assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        }

    @Test
    fun hiddenNamesKeepTheLockScreenVersionGeneric() =
        runBlocking {
            val data = ReferenceFixtures.data.copy(settings = Settings(hideNamesInReminders = true))
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
            notifications.showCourseDoses(CourseFixtures.data, LocalDateTime(ReferenceFixtures.date, LocalTime(16, 0)))

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
        }
}
