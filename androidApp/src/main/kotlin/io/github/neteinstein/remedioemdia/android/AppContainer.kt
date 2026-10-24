package io.github.neteinstein.remedioemdia.android

import android.content.Context
import io.github.neteinstein.remedioemdia.android.reminders.ReminderNotifications
import io.github.neteinstein.remedioemdia.android.reminders.ReminderScheduler
import io.github.neteinstein.remedioemdia.android.widgets.Widgets
import io.github.neteinstein.remedioemdia.core.data.FileDocumentStorage
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.schedule.newlyLowStock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** App-wide objects, created once per process by [RemedioApplication]. */
class AppContainer(
    private val context: Context,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = MedicationRepository(FileDocumentStorage(File(context.filesDir, "remedio-em-dia.json")))
    val reminders = ReminderScheduler(context)
    val notifications = ReminderNotifications(context)

    private val started = AtomicBoolean(false)

    /** Keeps alarms, widgets and low-stock warnings in step with every data change. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            repository.load()
            notifications.ensureChannels()
            var previous: AppData? = null
            repository.data.collect { data ->
                reminders.schedule(data)
                previous?.let { before -> data.newlyLowStock(before).forEach { notifications.showLowStock(it) } }
                previous = data
                Widgets.updateAll(context)
            }
        }
    }

    /** Re-evaluates alarms and widgets when time moved but the data did not (boot, clock change, midnight). */
    suspend fun refresh() {
        repository.load()
        reminders.schedule(repository.data.value)
        Widgets.updateAll(context)
    }
}
