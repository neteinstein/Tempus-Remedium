package org.mariacf.tempusremedium.android.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.android.widgets.Widgets
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.logSlot
import kotlin.time.Clock

/** Handles reminder alarms and the "Tomei" / "Adiar 10 min" notification actions. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val container = context.container
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse)
        val slot = intent.getStringExtra(EXTRA_SLOT)?.let(TimeSlot::valueOf)
        val result = goAsync()
        container.scope.launch {
            try {
                container.repository.load()
                when (intent.action) {
                    ACTION_SHOW -> {
                        if (date != null &&
                            slot != null
                        ) {
                            container.notifications.showSlot(container.repository.data.value, date, slot)
                        }
                    }

                    ACTION_TAKE -> {
                        if (date != null && slot != null) {
                            container.repository.update {
                                it.logSlot(
                                    date,
                                    slot,
                                    DoseStatus.TAKEN,
                                    Clock.System.now().toEpochMilliseconds(),
                                )
                            }
                            container.notifications.cancelSlot(slot)
                        }
                    }

                    ACTION_SNOOZE -> {
                        if (date != null && slot != null) {
                            container.notifications.cancelSlot(slot)
                            container.reminders.snooze(date, slot)
                        }
                    }

                    ACTION_DAY_CHANGED -> {
                        Widgets.updateAll(context)
                    }
                }
                container.reminders.schedule(container.repository.data.value)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "org.mariacf.tempusremedium.SHOW_REMINDER"
        const val ACTION_TAKE = "org.mariacf.tempusremedium.TAKE_SLOT"
        const val ACTION_SNOOZE = "org.mariacf.tempusremedium.SNOOZE_SLOT"
        const val ACTION_DAY_CHANGED = "org.mariacf.tempusremedium.DAY_CHANGED"
        const val EXTRA_DATE = "date"
        const val EXTRA_SLOT = "slot"

        private const val REQUEST_DAY_CHANGED = 3
        private const val REQUEST_TAKE_BASE = 10
        private const val REQUEST_SNOOZE_BASE = 20

        fun showIntent(
            context: Context,
            requestCode: Int,
            date: LocalDate?,
            slot: TimeSlot?,
        ): PendingIntent = broadcast(context, requestCode, ACTION_SHOW, date, slot)

        fun takeIntent(
            context: Context,
            date: LocalDate,
            slot: TimeSlot,
        ): PendingIntent = broadcast(context, REQUEST_TAKE_BASE + slot.ordinal, ACTION_TAKE, date, slot)

        fun snoozeIntent(
            context: Context,
            date: LocalDate,
            slot: TimeSlot,
        ): PendingIntent = broadcast(context, REQUEST_SNOOZE_BASE + slot.ordinal, ACTION_SNOOZE, date, slot)

        fun dayChangedIntent(context: Context): PendingIntent = broadcast(context, REQUEST_DAY_CHANGED, ACTION_DAY_CHANGED, null, null)

        private fun broadcast(
            context: Context,
            requestCode: Int,
            action: String,
            date: LocalDate?,
            slot: TimeSlot?,
        ): PendingIntent {
            val intent =
                Intent(context, ReminderReceiver::class.java)
                    .setAction(action)
                    .putExtra(EXTRA_DATE, date?.toString())
                    .putExtra(EXTRA_SLOT, slot?.name)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
