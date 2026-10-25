package org.mariacf.tempusremedium.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.mariacf.tempusremedium.android.settings.AndroidSettingsExtras
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.PlatformUi
import org.mariacf.tempusremedium.ui.screens.LaunchRequest

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    /** Refreshed on resume and after the permission dialog: the user can change it in system settings. */
    private var notificationsBlocked by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshNotificationsBlocked() }

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
                notificationsBlocked = { notificationsBlocked },
                requestNotifications = ::requestNotificationPermission,
                openNotificationSettings = ::openNotificationSettings,
            )
        setContent {
            App(
                repository = container.repository,
                platform = platform,
                launchRequest = launchRequest,
                onConsumeLaunchRequest = { launchRequest = null },
            )
        }
        refreshNotificationsBlocked()
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationsBlocked()
    }

    private fun refreshNotificationsBlocked() {
        notificationsBlocked = !container.notifications.canPost()
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

        fun Intent.toLaunchRequest(): LaunchRequest? {
            getStringExtra(EXTRA_SLOT)
                ?.let { name -> TimeSlot.entries.firstOrNull { it.name == name } }
                ?.let { return LaunchRequest.OpenSlot(it) }
            return getStringExtra(EXTRA_REFILL)?.let(LaunchRequest::OpenRefill)
        }
    }
}
