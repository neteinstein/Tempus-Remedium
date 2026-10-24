package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Widgets {
    private val refreshes = MutableStateFlow(0L)

    /** Bumped on every update so running widget sessions reload their data. */
    val refreshCount: StateFlow<Long> = refreshes.asStateFlow()

    suspend fun updateAll(context: Context) {
        refreshes.value++
        try {
            HojeWidget().updateAll(context)
            ReporEmBreveWidget().updateAll(context)
            CourseWidget().updateAll(context)
        } catch (e: IllegalStateException) {
            // Glance needs WorkManager; never let a widget failure break reminders.
            Log.w("Widgets", "Could not update widgets", e)
        }
    }
}
