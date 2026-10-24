package io.github.neteinstein.remedioemdia.android.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.neteinstein.remedioemdia.android.container
import kotlinx.coroutines.launch

/** Alarms do not survive reboots, and clock or time-zone changes move every slot. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in ACTIONS) return
        val container = context.container
        val result = goAsync()
        container.scope.launch {
            try {
                container.refresh()
            } finally {
                result.finish()
            }
        }
    }

    private companion object {
        val ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            )
    }
}
