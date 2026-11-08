package org.mariacf.tempusremedium.android.reminders

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import org.mariacf.tempusremedium.android.R

/**
 * Rings an alarm reminder on the alarm stream until it is handled.
 *
 * A notification channel's sound plays on the notification stream's rules: silent and vibrate
 * mode, and on many phones Do Not Disturb or bedtime mode while charging, mute it whatever its
 * audio usage. The alarm stream is not muted by the ringer mode and passes Do Not Disturb when
 * alarms are allowed (the default), so the app plays the tone itself, like an alarm clock. The
 * reminder becomes this service's notification; the sound stops when it is taken, snoozed,
 * dismissed or the app is opened.
 */
class AlarmRingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var foregroundId: Int? = null
    private val ringing = mutableSetOf<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_RING -> ring(intent)
            else -> stop(intent?.getIntExtra(EXTRA_CANCEL_ID, NO_ID) ?: NO_ID)
        }
        return START_NOT_STICKY
    }

    private fun ring(intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, NO_ID)
        val notification = IntentCompat.getParcelableExtra(intent, EXTRA_NOTIFICATION, Notification::class.java)
        if (notification == null) {
            // startForegroundService() still needs a startForeground() call before stopping.
            ServiceCompat.startForeground(this, PLACEHOLDER_ID, placeholder(), FOREGROUND_TYPE)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stop(NO_ID)
            return
        }
        val current = foregroundId
        if (current == null || current == id) {
            ServiceCompat.startForeground(this, id, notification, FOREGROUND_TYPE)
            foregroundId = id
        } else {
            // A second reminder while ringing: a new foreground id would cancel the first one.
            // Only reached once ReminderNotifications checked POST_NOTIFICATIONS.
            try {
                NotificationManagerCompat.from(this).notify(id, notification)
            } catch (_: SecurityException) {
                // Permission revoked in between: nothing to show.
            }
        }
        ringing += id
        running = true
        play(intent.getStringExtra(EXTRA_SOUND))
        vibrate()
    }

    /**
     * Stops ringing and leaves the reminders on screen, except [cancelId] when given. A reminder
     * that is not ringing is only cancelled.
     */
    private fun stop(cancelId: Int) {
        if (cancelId != NO_ID && ringing.isNotEmpty() && cancelId !in ringing) {
            NotificationManagerCompat.from(this).cancel(cancelId)
            return
        }
        ringing.clear()
        running = false
        release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        foregroundId = null
        if (cancelId != NO_ID) NotificationManagerCompat.from(this).cancel(cancelId)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        release()
        super.onDestroy()
    }

    private fun play(chosen: String?) {
        player?.release()
        player =
            candidates(chosen).firstNotNullOfOrNull { uri ->
                runCatching {
                    MediaPlayer().apply {
                        setAudioAttributes(ALARM_AUDIO)
                        setDataSource(this@AlarmRingService, uri)
                        isLooping = true
                        prepare()
                        start()
                    }
                }.getOrNull()
            }
    }

    private fun candidates(chosen: String?): List<Uri> =
        listOfNotNull(
            chosen?.toUri(),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        ).distinct()

    private fun vibrate() {
        val device =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
        if (device == null || !device.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(VIBRATION, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            device.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            device.vibrate(effect, ALARM_AUDIO)
        }
        vibrator = device
    }

    private fun release() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun placeholder(): Notification =
        NotificationCompat
            .Builder(this, ReminderNotifications.CHANNEL_REMINDERS_RINGING)
            .setSmallIcon(R.drawable.ic_notification)
            .build()

    companion object {
        private const val ACTION_RING = "org.mariacf.tempusremedium.RING"
        private const val ACTION_STOP = "org.mariacf.tempusremedium.STOP_RINGING"
        private const val EXTRA_ID = "id"
        private const val EXTRA_NOTIFICATION = "notification"
        private const val EXTRA_SOUND = "sound"
        private const val EXTRA_CANCEL_ID = "cancel_id"
        private const val NO_ID = Int.MIN_VALUE
        private const val PLACEHOLDER_ID = 999

        private val FOREGROUND_TYPE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0

        val ALARM_AUDIO: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        private val VIBRATION = longArrayOf(0, 800, 600, 800, 600)

        /** True while a reminder is ringing in this process. */
        @Volatile
        var running = false
            private set

        /**
         * Shows [notification] as reminder [id] and rings [sound] (null: the device's alarm sound).
         * False when Android does not let the app start it now (no exact alarm behind this call):
         * the caller then posts the reminder on a channel with its own sound.
         */
        fun ring(
            context: Context,
            id: Int,
            notification: Notification,
            sound: String?,
        ): Boolean {
            val intent =
                Intent(context, AlarmRingService::class.java)
                    .setAction(ACTION_RING)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_NOTIFICATION, notification)
                    .putExtra(EXTRA_SOUND, sound)
            val started = runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
            if (started) running = true
            return started
        }

        /** Silences the ringing reminder, if any, and cancels reminder [cancelId] when given. */
        fun silence(
            context: Context,
            cancelId: Int? = null,
        ) {
            if (!running) return
            val intent =
                Intent(context, AlarmRingService::class.java)
                    .setAction(ACTION_STOP)
                    .putExtra(EXTRA_CANCEL_ID, cancelId ?: NO_ID)
            runCatching { context.startService(intent) }
        }
    }
}
