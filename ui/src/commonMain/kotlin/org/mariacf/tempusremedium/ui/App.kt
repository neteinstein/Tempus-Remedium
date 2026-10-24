package org.mariacf.tempusremedium.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.ThemeMode
import org.mariacf.tempusremedium.core.schedule.activeCourses
import org.mariacf.tempusremedium.core.schedule.adherence
import org.mariacf.tempusremedium.core.schedule.lowStock
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.nav_history
import org.mariacf.tempusremedium.ui.resources.nav_medications
import org.mariacf.tempusremedium.ui.resources.nav_settings
import org.mariacf.tempusremedium.ui.resources.nav_today
import org.mariacf.tempusremedium.ui.screens.CourseEditor
import org.mariacf.tempusremedium.ui.screens.HistoryScreen
import org.mariacf.tempusremedium.ui.screens.LaunchRequest
import org.mariacf.tempusremedium.ui.screens.MedicationEditor
import org.mariacf.tempusremedium.ui.screens.MedicationsScreen
import org.mariacf.tempusremedium.ui.screens.SettingsScreen
import org.mariacf.tempusremedium.ui.screens.TodayScreen
import org.mariacf.tempusremedium.ui.theme.TempusTheme

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
    /** Lets the platform match its system bars to the theme in use. */
    val onDarkThemeChange: (Boolean) -> Unit = {},
)

/** Whether [ThemeMode] resolves to dark right now. */
@Composable
fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

@Composable
fun App(
    repository: MedicationRepository,
    modifier: Modifier = Modifier,
    platform: PlatformUi = remember { PlatformUi() },
    launchRequest: LaunchRequest? = null,
    onConsumeLaunchRequest: () -> Unit = {},
    viewModel: AppViewModel = viewModel { AppViewModel(repository) },
) {
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

    val dark = data.settings.theme.isDark()
    val onDarkThemeChange by rememberUpdatedState(platform.onDarkThemeChange)
    LaunchedEffect(dark) { onDarkThemeChange(dark) }

    TempusTheme(darkTheme = dark) {
        val palette = TempusTheme.colors
        Box(modifier.fillMaxSize().brandBackground(palette)) {
            val editing = editingId
            val editingMedication = editing?.let(data::medication)
            if (editing == NEW_COURSE || editingMedication?.course != null) {
                CourseEditor(
                    medication = editingMedication,
                    now = now,
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
                    Column {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
                        NavigationBar(containerColor = palette.surfaceStrong, tonalElevation = 0.dp) {
                            Tab.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = item == tab,
                                    onClick = { tab = item },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = {
                                        Text(
                                            stringResource(item.title),
                                            style = TempusTheme.type.eyebrow.copy(letterSpacing = 0.2.sp),
                                        )
                                    },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = palette.accent,
                                            selectedTextColor = palette.accent,
                                            indicatorColor = palette.accentSoft,
                                            unselectedIconColor = palette.inkMute,
                                            unselectedTextColor = palette.inkMute,
                                        ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.padding(padding),
                ) { current ->
                    when (current) {
                        Tab.TODAY -> {
                            TodayScreen(
                                now = now,
                                plan = data.planFor(now.date),
                                lowStock = data.lowStock(),
                                medications = data.activeMedications,
                                stockDisplay = data.settings.stockDisplay,
                                onLogDose = { medication, slot, status -> viewModel.logDose(medication.id, now.date, slot, status) },
                                onUndoDose = { medication, slot -> viewModel.undoDose(medication.id, now.date, slot) },
                                onLogSlot = { slot -> viewModel.logSlot(now.date, slot) },
                                onRefill = { medication, units -> viewModel.refill(medication.id, units) },
                                onAddMedication = { editingId = NEW },
                                courses = data.activeCourses(now),
                                onLogCourseDose = { medication, index, status -> viewModel.logCourseDose(medication.id, index, status) },
                                onUndoCourseDose = { medication, index -> viewModel.undoCourseDose(medication.id, index) },
                                onAddCourse = { editingId = NEW_COURSE },
                                launchRequest = launchRequest,
                                onConsumeLaunchRequest = onConsumeLaunchRequest,
                            )
                        }

                        Tab.MEDICATIONS -> {
                            MedicationsScreen(
                                medications = data.medications,
                                onEdit = { editingId = it.id },
                                onAdd = { editingId = NEW },
                                onAddCourse = { editingId = NEW_COURSE },
                            )
                        }

                        Tab.HISTORY -> {
                            HistoryScreen(data.adherence(now))
                        }

                        Tab.SETTINGS -> {
                            SettingsScreen(
                                settings = data.settings,
                                onChange = viewModel::updateSettings,
                                extras = { platform.settingsExtras(this, data.settings, viewModel::updateSettings) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val NEW = "new"
private const val NEW_COURSE = "new_course"
