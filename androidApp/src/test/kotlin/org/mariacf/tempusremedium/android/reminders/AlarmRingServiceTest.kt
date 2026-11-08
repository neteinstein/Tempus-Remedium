package org.mariacf.tempusremedium.android.reminders

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.android.R
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT")
class AlarmRingServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val system = context.getSystemService(NotificationManager::class.java)

    @Before
    fun channels() = runBlocking { ReminderNotifications(context).ensureChannels() }

    private fun reminder(title: String): Notification =
        NotificationCompat
            .Builder(context, ReminderNotifications.CHANNEL_REMINDERS_RINGING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .build()

    /** Starts the service the way [AlarmRingService.ring] does and returns its controller. */
    private fun ring(
        id: Int,
        title: String,
    ): ServiceController<AlarmRingService> {
        assertTrue(AlarmRingService.ring(context, id, reminder(title), null))
        val intent = shadowOf(context).nextStartedService
        return Robolectric.buildService(AlarmRingService::class.java, intent).create().startCommand(0, 1)
    }

    /** The intent [AlarmRingService.silence] sends. */
    private fun silenceIntent(): Intent = shadowOf(context).nextStartedService

    @Test
    fun theRingingReminderIsTheForegroundNotification() {
        val service = ring(100, "Manhã · 08:00").get()

        assertTrue(AlarmRingService.running)
        assertEquals("Manhã · 08:00", shadowOf(service).lastForegroundNotification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(100, shadowOf(service).lastForegroundNotificationId)
    }

    @Test
    fun takingTheDoseStopsTheSoundAndRemovesTheReminder() {
        val controller = ring(100, "Manhã · 08:00")

        AlarmRingService.silence(context, 100)
        controller.withIntent(silenceIntent()).startCommand(0, 2)

        val service = shadowOf(controller.get())
        assertFalse(AlarmRingService.running)
        assertTrue(service.isForegroundStopped)
        assertTrue(service.isStoppedBySelf)
        assertNull(shadowOf(system).getNotification(100))
    }

    @Test
    fun dismissingOrOpeningTheAppOnlyStopsTheSound() {
        val controller = ring(100, "Manhã · 08:00")

        AlarmRingService.silence(context)
        controller.withIntent(silenceIntent()).startCommand(0, 2)

        assertFalse(AlarmRingService.running)
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
    }

    @Test
    fun aSecondReminderWhileRingingKeepsTheFirst() {
        val controller = ring(100, "Manhã · 08:00")
        AlarmRingService.ring(context, 200, reminder("Amoxicilina · 08:00"), null)
        controller.withIntent(shadowOf(context).nextStartedService).startCommand(0, 2)

        assertEquals(100, shadowOf(controller.get()).lastForegroundNotificationId)
        assertNotNull(shadowOf(system).getNotification(200))
    }

    @Test
    fun cancellingAnotherReminderKeepsRinging() {
        val controller = ring(100, "Manhã · 08:00")

        AlarmRingService.silence(context, 101)
        controller.withIntent(silenceIntent()).startCommand(0, 2)

        assertTrue(AlarmRingService.running)
        assertFalse(shadowOf(controller.get()).isStoppedBySelf)
    }

    @Test
    fun nothingIsSentWhenNothingRings() {
        ring(100, "Manhã · 08:00").also {
            AlarmRingService.silence(context)
            it.withIntent(silenceIntent()).startCommand(0, 2)
        }

        AlarmRingService.silence(context)
        assertNull(shadowOf(context).nextStartedService)
    }
}
