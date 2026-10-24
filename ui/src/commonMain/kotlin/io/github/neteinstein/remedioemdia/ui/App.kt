package io.github.neteinstein.remedioemdia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.core.schedule.adherence
import io.github.neteinstein.remedioemdia.core.schedule.lowStock
import io.github.neteinstein.remedioemdia.core.schedule.planFor
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.nav_history
import io.github.neteinstein.remedioemdia.ui.resources.nav_medications
import io.github.neteinstein.remedioemdia.ui.resources.nav_settings
import io.github.neteinstein.remedioemdia.ui.resources.nav_today
import io.github.neteinstein.remedioemdia.ui.screens.HistoryScreen
import io.github.neteinstein.remedioemdia.ui.screens.LaunchRequest
import io.github.neteinstein.remedioemdia.ui.screens.MedicationEditor
import io.github.neteinstein.remedioemdia.ui.screens.MedicationsScreen
import io.github.neteinstein.remedioemdia.ui.screens.SettingsScreen
import io.github.neteinstein.remedioemdia.ui.screens.TodayScreen
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class Tab(
    val title: StringResource,
    val icon: ImageVector,
) {
    TODAY(Res.string.nav_today, Icons.Filled.DateRange),
    MEDICATIONS(Res.string.nav_medications, Icons.AutoMirrored.Filled.List),
    HISTORY(Res.string.nav_history, Icons.Filled.CheckCircle),
    SETTINGS(Res.string.nav_settings, Icons.Filled.Settings),
}

/** What a platform adds to the shared app (Android: reminders and widgets; web: storage notice). */
class PlatformUi(
    val settingsExtras: @Composable ColumnScope.(Settings, (Settings) -> Unit) -> Unit = { _, _ -> },
)

@Composable
fun App(
    repository: MedicationRepository,
    modifier: Modifier = Modifier,
    platform: PlatformUi = remember { PlatformUi() },
    launchRequest: LaunchRequest? = null,
    onConsumeLaunchRequest: () -> Unit = {},
) {
    val viewModel = viewModel { AppViewModel(repository) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    // null = closed, NEW = adding, otherwise the id being edited.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(launchRequest) {
        if (launchRequest != null) {
            tab = Tab.TODAY
            editingId = null
        }
    }

    RemedioTheme {
        Box(modifier.fillMaxSize().background(RemedioColors.Background)) {
            val editing = editingId
            if (editing != null) {
                MedicationEditor(
                    medication = data.medication(editing),
                    today = now.date,
                    onSave = { medication: Medication ->
                        viewModel.saveMedication(medication)
                        editingId = null
                    },
                    onDelete = { medication ->
                        viewModel.deleteMedication(medication.id)
                        editingId = null
                    },
                    onClose = { editingId = null },
                )
                return@Box
            }
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = RemedioColors.Surface.copy(alpha = 0.92f)) {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == tab,
                                onClick = { tab = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.title)) },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor = RemedioColors.Amber,
                                        selectedTextColor = RemedioColors.Amber,
                                        indicatorColor = RemedioColors.GlassFill,
                                        unselectedIconColor = RemedioColors.TextSecondary,
                                        unselectedTextColor = RemedioColors.TextSecondary,
                                    ),
                            )
                        }
                    }
                },
            ) { padding ->
                val content = Modifier.padding(padding)
                when (tab) {
                    Tab.TODAY -> {
                        TodayScreen(
                            plan = data.planFor(now.date),
                            lowStock = data.lowStock(),
                            medications = data.activeMedications,
                            onLogDose = { medication, slot, status -> viewModel.logDose(medication.id, now.date, slot, status) },
                            onUndoDose = { medication, slot -> viewModel.undoDose(medication.id, now.date, slot) },
                            onLogSlot = { slot -> viewModel.logSlot(now.date, slot) },
                            onRefill = { medication, units -> viewModel.refill(medication.id, units) },
                            onAddMedication = { editingId = NEW },
                            launchRequest = launchRequest,
                            onConsumeLaunchRequest = onConsumeLaunchRequest,
                            modifier = content,
                        )
                    }

                    Tab.MEDICATIONS -> {
                        MedicationsScreen(
                            medications = data.medications,
                            onEdit = { editingId = it.id },
                            onAdd = { editingId = NEW },
                            modifier = content,
                        )
                    }

                    Tab.HISTORY -> {
                        HistoryScreen(data.adherence(now), modifier = content)
                    }

                    Tab.SETTINGS -> {
                        SettingsScreen(
                            settings = data.settings,
                            onChange = viewModel::updateSettings,
                            modifier = content,
                            extras = { platform.settingsExtras(this, data.settings, viewModel::updateSettings) },
                        )
                    }
                }
            }
        }
    }
}

private const val NEW = "new"
