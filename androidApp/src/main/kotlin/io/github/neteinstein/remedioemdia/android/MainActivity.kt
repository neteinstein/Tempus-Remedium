package io.github.neteinstein.remedioemdia.android

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
import io.github.neteinstein.remedioemdia.android.settings.AndroidSettingsExtras
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.ui.App
import io.github.neteinstein.remedioemdia.ui.PlatformUi
import io.github.neteinstein.remedioemdia.ui.screens.LaunchRequest

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
            )
        setContent {
            App(
                repository = container.repository,
                platform = platform,
                launchRequest = launchRequest,
                onConsumeLaunchRequest = { launchRequest = null },
            )
        }
        requestNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchRequest()?.let { launchRequest = it }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_REFILL = "refill"

        fun Intent.toLaunchRequest(): LaunchRequest? {
            getStringExtra(EXTRA_SLOT)
                ?.let { name -> TimeSlot.entries.firstOrNull { it.name == name } }
                ?.let { return LaunchRequest.OpenSlot(it) }
            return getStringExtra(EXTRA_REFILL)?.let(LaunchRequest::OpenRefill)
        }
    }
}
