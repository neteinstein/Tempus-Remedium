package org.mariacf.tempusremedium.android.reminders

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.android.MainActivity
import org.mariacf.tempusremedium.android.R
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.LowStockItem
import org.mariacf.tempusremedium.core.schedule.courseDoseCount
import org.mariacf.tempusremedium.core.schedule.courseDosesOn
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.formatDecimal
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_taken
import org.mariacf.tempusremedium.ui.resources.channel_low_stock
import org.mariacf.tempusremedium.ui.resources.channel_reminders
import org.mariacf.tempusremedium.ui.resources.channel_reminders_alarm
import org.mariacf.tempusremedium.ui.resources.course_dose_of
import org.mariacf.tempusremedium.ui.resources.decimal_separator
import org.mariacf.tempusremedium.ui.resources.dose_ml
import org.mariacf.tempusremedium.ui.resources.dose_units
import org.mariacf.tempusremedium.ui.resources.notification_hidden_body
import org.mariacf.tempusremedium.ui.resources.notification_low_stock_body
import org.mariacf.tempusremedium.ui.resources.notification_slot_title
import org.mariacf.tempusremedium.ui.resources.notification_snooze
import org.mariacf.tempusremedium.ui.resources.refill_soon
import org.mariacf.tempusremedium.ui.resources.units_left
import kotlin.math.ceil

class ReminderNotifications(
    private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    suspend fun ensureChannels() {
        manager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(getString(Res.string.channel_reminders))
                .build(),
        )
        // A channel's sound cannot change once created, so the alarm style has its own channel.
        manager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_REMINDERS_ALARM, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(getString(Res.string.channel_reminders_alarm))
                .setSound(alarmSound(), ALARM_AUDIO)
                .setVibrationEnabled(true)
                .setVibrationPattern(ALARM_VIBRATION)
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_LOW_STOCK, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(Res.string.channel_low_stock))
                .build(),
        )
    }

    fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            )

    /** "Manhã · 08:00 — Metformina 500mg, Vitamina D 1000IU" with Tomei / Adiar 10 min. */
    suspend fun showSlot(
        data: AppData,
        date: LocalDate,
        slot: TimeSlot,
    ) {
        val plan = data.planFor(date).slot(slot)
        val pending = plan.doses.filter { it.status == null }
        if (pending.isEmpty() || !data.settings.remindersEnabled) return
        ensureChannels()

        val title = getString(Res.string.notification_slot_title, getString(slot.label), plan.time.hhmm())
        val names = pending.joinToString(", ") { it.medication.displayName }
        val hidden = getPluralString(Res.plurals.notification_hidden_body, pending.size, pending.size)
        val builder =
            NotificationCompat
                .Builder(context, reminderChannel(data))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(names)
                .setStyle(NotificationCompat.BigTextStyle().bigText(names))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openApp(MainActivity.EXTRA_SLOT, slot.name, slot.ordinal))
                .addAction(0, getString(Res.string.action_taken), ReminderReceiver.takeIntent(context, date, slot))
                .addAction(0, getString(Res.string.notification_snooze), ReminderReceiver.snoozeIntent(context, date, slot))
        if (data.settings.hideNamesInReminders) {
            builder
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat
                        .Builder(context, CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(hidden)
                        .build(),
                )
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        post(slotNotificationId(slot), builder, insistent = data.settings.alarmReminders)
    }

    fun cancelSlot(slot: TimeSlot) = manager.cancel(slotNotificationId(slot))

    /** "Amoxicilina · 16:00 — 6 mL · toma 2 de 21" with Tomei / Adiar 10 min. */
    suspend fun showCourseDoses(
        data: AppData,
        at: LocalDateTime,
    ) {
        val pending = data.courseDosesOn(at.date).filter { it.at == at && it.status == null }
        if (pending.isEmpty() || !data.settings.remindersEnabled) return
        ensureChannels()

        val time = at.time.hhmm()
        val title = getString(Res.string.notification_slot_title, pending.joinToString(", ") { it.medication.displayName }, time)
        val body =
            pending
                .map { dose ->
                    val course = dose.medication.course!!
                    val amount =
                        when (course.unit) {
                            DoseUnit.ML -> {
                                getString(Res.string.dose_ml, formatDecimal(course.amount, getString(Res.string.decimal_separator)))
                            }

                            DoseUnit.UNITS -> {
                                val units = ceil(course.amount).toInt().coerceAtLeast(1)
                                getPluralString(
                                    Res.plurals.dose_units,
                                    units,
                                    formatDecimal(course.amount, getString(Res.string.decimal_separator)),
                                )
                            }
                        }
                    getString(Res.string.course_dose_of, amount, dose.index + 1, data.courseDoseCount(dose.medication))
                }.joinToString("\n")
        val hidden = getPluralString(Res.plurals.notification_hidden_body, pending.size, pending.size)
        val builder =
            NotificationCompat
                .Builder(context, reminderChannel(data))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openApp(EXTRA_NONE, "", COURSE_REQUEST))
                .addAction(0, getString(Res.string.action_taken), ReminderReceiver.takeCourseIntent(context, at))
                .addAction(0, getString(Res.string.notification_snooze), ReminderReceiver.snoozeCourseIntent(context, at))
        if (data.settings.hideNamesInReminders) {
            builder
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat
                        .Builder(context, CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(time)
                        .setContentText(hidden)
                        .build(),
                )
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        post(COURSE_NOTIFICATION_ID, builder, insistent = data.settings.alarmReminders)
    }

    fun cancelCourseDoses() = manager.cancel(COURSE_NOTIFICATION_ID)

    /** "Repor em breve — Ómega 3 · Restam 3" */
    suspend fun showLowStock(item: LowStockItem) {
        ensureChannels()
        val count = getPluralString(Res.plurals.units_left, item.remaining, item.remaining)
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_LOW_STOCK)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(Res.string.refill_soon))
                .setContentText(getString(Res.string.notification_low_stock_body, item.medication.name, count))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(openApp(MainActivity.EXTRA_REFILL, item.medication.id, LOW_STOCK_REQUEST + item.medication.id.hashCode()))
        post(LOW_STOCK_ID_BASE + item.medication.id.hashCode(), builder)
    }

    private fun reminderChannel(data: AppData) = if (data.settings.alarmReminders) CHANNEL_REMINDERS_ALARM else CHANNEL_REMINDERS

    private fun alarmSound() =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /** [insistent] repeats the sound until the reminder is acted on, dismissed or the shade is opened. */
    private fun post(
        id: Int,
        builder: NotificationCompat.Builder,
        insistent: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = builder.build()
        if (insistent) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        manager.notify(id, notification)
    }

    private fun openApp(
        extra: String,
        value: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(extra, value)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_REMINDERS_ALARM = "reminders_alarm"
        const val CHANNEL_LOW_STOCK = "low_stock"
        private val ALARM_AUDIO =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        private val ALARM_VIBRATION = longArrayOf(0, 800, 600, 800, 600)
        private const val LOW_STOCK_ID_BASE = 1_000
        private const val LOW_STOCK_REQUEST = 100
        private const val COURSE_REQUEST = 200
        const val COURSE_NOTIFICATION_ID = 200
        private const val EXTRA_NONE = "none"

        fun slotNotificationId(slot: TimeSlot) = 100 + slot.ordinal
    }
}
