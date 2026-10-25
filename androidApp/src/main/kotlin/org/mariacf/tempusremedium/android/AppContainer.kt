package org.mariacf.tempusremedium.android

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mariacf.tempusremedium.android.reminders.ReminderNotifications
import org.mariacf.tempusremedium.android.reminders.ReminderScheduler
import org.mariacf.tempusremedium.android.widgets.Widgets
import org.mariacf.tempusremedium.core.data.FileDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.AppLanguage
import org.mariacf.tempusremedium.core.schedule.newlyLowStock
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** App-wide objects, created once per process by [TempusRemediumApplication]. */
class AppContainer(
    private val context: Context,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = MedicationRepository(FileDocumentStorage(File(context.filesDir, "tempus-remedium.json")))
    val reminders = ReminderScheduler(context)
    val notifications = ReminderNotifications(context)

    private val started = AtomicBoolean(false)

    /** Keeps alarms, widgets and low-stock warnings in step with every data change. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            repository.load()
            var previous: AppData? = null
            repository.data.collect { data ->
                applyLanguage(data.settings.language)
                if (previous == null || previous?.settings?.alarmSound != data.settings.alarmSound) {
                    notifications.ensureChannels(data.settings)
                }
                reminders.schedule(data)
                previous?.let { before -> data.newlyLowStock(before).forEach { notifications.showLowStock(it) } }
                previous = data
                Widgets.updateAll(context)
            }
        }
    }

    /**
     * Notifications and widgets read strings outside any composition, from the default locale,
     * so it follows the language setting even when no screen is open.
     */
    private fun applyLanguage(language: AppLanguage) {
        val device = Resources.getSystem().configuration.locales[0]
        Locale.setDefault(language.tag?.let(Locale::forLanguageTag) ?: device)
    }

    /** Re-evaluates alarms and widgets when time moved but the data did not (boot, clock change, midnight). */
    suspend fun refresh() {
        repository.load()
        applyLanguage(repository.data.value.settings.language)
        reminders.schedule(repository.data.value)
        Widgets.updateAll(context)
    }
}
