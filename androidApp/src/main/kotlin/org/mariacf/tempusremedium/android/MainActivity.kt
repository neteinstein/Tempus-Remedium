package org.mariacf.tempusremedium.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.mariacf.tempusremedium.android.settings.AndroidSettingsExtras
import org.mariacf.tempusremedium.android.widgets.AndroidWidgetSupport
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.PlatformUi
import org.mariacf.tempusremedium.ui.screens.LaunchRequest

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    /** Refreshed on resume and after the permission dialog: the user can change them in system settings. */
    private var permissionsMissing by mutableStateOf(false)

    /** When the notification dialog was requested, to tell a real answer from an instant refusal. */
    private var askedAt = 0L

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Refused with no dialog on screen: the system stopped asking (e.g. denied twice in an
            // older version, before we kept track). Only system settings can turn it on now.
            if (!granted && SystemClock.elapsedRealtime() - askedAt < NO_DIALOG_MILLIS) openNotificationSettings()
            refreshPermissions()
        }

    private val files = AndroidFileTransfer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launchRequest = intent.toLaunchRequest()

        val platform =
            PlatformUi(
                settingsExtras = { settings, onChange ->
                    AndroidSettingsExtras(
                        settings = settings,
                        onChange = onChange,
                        onRequestNotifications = ::requestNotificationPermission,
                        onRequestExactAlarms = ::openExactAlarmSettings,
                    )
                },
                onDarkThemeChange = { dark ->
                    // Light status-bar icons on the dark theme, dark icons on the light one.
                    val style =
                        if (dark) {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                },
                permissionsMissing = { permissionsMissing },
                requestPermissions = ::requestMissingPermission,
                widgets = AndroidWidgetSupport(applicationContext),
                files = files,
            )
        setContent {
            App(
                repository = container.repository,
                platform = platform,
                launchRequest = launchRequest,
                onConsumeLaunchRequest = { launchRequest = null },
            )
        }
        refreshPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        permissionsMissing = !container.notifications.canPost() || !container.reminders.canScheduleExact()
    }

    /** Notifications first (the system dialog), then exact alarms (their settings screen). */
    private fun requestMissingPermission() {
        when {
            !container.notifications.canPost() -> requestNotificationPermission()
            !container.reminders.canScheduleExact() -> openExactAlarmSettings()
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        startActivity(
            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchRequest()?.let { launchRequest = it }
    }

    /** Asked when the first medication is saved (after the app explains why), or from Definições. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !permissionDeniedForGood()
        ) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ASKED_KEY, true).apply()
            askedAt = SystemClock.elapsedRealtime()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Denied twice (the system no longer asks) or turned off: only system settings can change it.
            openNotificationSettings()
        }
    }

    private fun permissionDeniedForGood(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ASKED_KEY, false) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    private fun openNotificationSettings() {
        startActivity(
            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_REFILL = "refill"
        private const val PREFS = "permissions"
        private const val ASKED_KEY = "notifications_asked"

        /** Faster than anyone can read and answer the permission dialog. */
        private const val NO_DIALOG_MILLIS = 500L

        fun Intent.toLaunchRequest(): LaunchRequest? {
            getStringExtra(EXTRA_SLOT)
                ?.let { name -> TimeSlot.entries.firstOrNull { it.name == name } }
                ?.let { return LaunchRequest.OpenSlot(it) }
            return getStringExtra(EXTRA_REFILL)?.let(LaunchRequest::OpenRefill)
        }
    }
}
