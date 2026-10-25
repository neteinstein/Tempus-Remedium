package org.mariacf.tempusremedium.android.store

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.android.R
import org.mariacf.tempusremedium.android.settings.AndroidSettingsExtras
import org.mariacf.tempusremedium.android.widgets.CourseWidgetContent
import org.mariacf.tempusremedium.android.widgets.CourseWidgetUi
import org.mariacf.tempusremedium.android.widgets.HojeWidgetContent
import org.mariacf.tempusremedium.android.widgets.HojeWidgetUi
import org.mariacf.tempusremedium.android.widgets.ReporWidgetContent
import org.mariacf.tempusremedium.android.widgets.ReporWidgetUi
import org.mariacf.tempusremedium.android.widgets.WidgetStyle
import org.mariacf.tempusremedium.android.widgets.renderWidget
import org.mariacf.tempusremedium.core.CourseFixtures
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.data.InMemoryDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.AppLanguage
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.core.model.ThemeMode
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.DayPlan
import org.mariacf.tempusremedium.core.schedule.logCourseDose
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.logSlot
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.AppViewModel
import org.mariacf.tempusremedium.ui.PlatformUi
import org.mariacf.tempusremedium.ui.components.HojePanel
import org.mariacf.tempusremedium.ui.pill.PillIcon
import org.mariacf.tempusremedium.ui.screens.LaunchRequest
import org.mariacf.tempusremedium.ui.theme.TempusPalette
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlarmManager
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Raw material for the Play listing and the demo video in store-listing/: the real app with
 * sample data, the feature graphic, the 512 px icon and the medication forms. Not a test: it only
 * runs with STORE_LISTING=1, so CI skips it. store-listing/scripts/ turns these captures into the
 * files to upload and the video; see store-listing/README.md for the commands.
 *
 * One subclass per listing language. Store copy (the feature graphic tagline) lives here rather
 * than in the app's string resources because the app never shows it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class StoreListingCaptures(
    private val locale: String,
    /** Set in the sample settings: the app resolves the device language once per process. */
    private val language: AppLanguage,
    /** Medication id → name in this language; the sample data is user content, so it is translated too. */
    private val names: Map<String, String>,
    private val tabs: Map<String, String>,
    private val tagline: String,
    private val byline: String,
    /** The reference day as a launcher writes it above the widgets. */
    private val homeDate: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    private val zone = TimeZone.of("Europe/Lisbon")
    private val date = ReferenceFixtures.date
    private var systemZone: java.util.TimeZone? = null

    @Before
    fun onlyOnRequest() {
        assumeTrue("Set STORE_LISTING=1 to render the Play listing graphics", System.getenv("STORE_LISTING") == "1")
        // "Tomado às 08:05" is formatted in the system zone.
        systemZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone.id))
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun restoreZone() {
        systemZone?.let(java.util.TimeZone::setDefault)
    }

    /** Middle of the day: MANHÃ taken, MEIO-DIA up next. */
    @Test
    fun today() = todayScreen()

    /**
     * 13:40 with MEIO-DIA not taken: its card is red, and Ómega 3, down to its last unit, pulses
     * red. Tests never run infinite animations, so the pulse stays at its start (alpha 0.15).
     */
    @Test
    fun overdue() =
        screen("02_overdue", at = LocalTime(13, 40)) {
            morningTaken().withStock(ReferenceFixtures.omega3.id, Stock(remaining = 1))
        }

    /**
     * The same screen with the alert already seen, so without the glow. The glow is alpha-blended,
     * so the scripts get any phase of the pulse from the pair: base + (glow - base) * pulse / 0.15.
     */
    @Test
    fun overdueSeen() =
        screen("02_overdue_base", at = LocalTime(13, 40)) {
            morningTaken().withStock(ReferenceFixtures.omega3.id, Stock(remaining = 1, alertSeenOn = date))
        }

    /** The MANHÃ sheet with one dose taken and one to take. */
    @Test
    fun slotSheet() = slotScreen()

    /** Every kind of medication: tablets, an inhaler, a patch, a nasal spray, drops and an injection. */
    @Test
    fun medications() = medicationsScreen()

    /** An antibiotic every 8 hours next to the daily medications, its 16:00 dose due. */
    @Test
    fun course() =
        screen("05_course", at = LocalTime(16, 5)) {
            morningTaken()
                .logSlot(date, TimeSlot.MIDDAY, DoseStatus.TAKEN, millis(date, TimeSlot.MIDDAY))
                .withCourse(dosesTaken = 1)
        }

    @Test
    fun history() = historyScreen()

    /** Evening, dark theme, blister packs: only DEITAR left. */
    @Test
    fun todayDark() =
        screen("07_today_dark", at = LocalTime(21, 30)) {
            eveningDone().run { copy(settings = settings.copy(theme = ThemeMode.DARK, stockDisplay = StockDisplay.BLISTERS)) }
        }

    /** Scrolled to the end: reminders, permissions and the credit to the author. */
    @Test
    fun settings() =
        screen("08_settings", at = LocalTime(12, 40), tab = "settings", scrollToEnd = true) {
            sample().run { copy(settings = settings.copy(hideNamesInReminders = true)) }
        }

    /** Video: a minute before MANHÃ, nothing taken yet. */
    @Test
    fun videoBeforeMorning() = screen("video_0759", at = LocalTime(7, 59)) { sample() }

    /** Video: right after "Tomei" on the MANHÃ reminder. */
    @Test
    fun videoMorningTaken() =
        screen("video_0801", at = LocalTime(8, 1)) {
            sample().logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, millis(date, 8, 0))
        }

    /** HOJE on the lock screen at midday, large, under the clock. */
    @Test
    fun lockScreenWidget() = lockScreen("09_lock_screen", at = LocalTime(12, 40)) { morningTaken() }

    /** The same widgets at night, in the dark theme and with blister packs. */
    @Test
    @Config(qualifiers = "+night")
    fun widgetsDark() =
        homeScreen("10_widgets_dark", at = LocalTime(21, 30)) {
            eveningDone()
                .withCourse(dosesTaken = 2)
                .run { copy(settings = settings.copy(stockDisplay = StockDisplay.BLISTERS)) }
        }

    // Tablets, in landscape: the app has no tablet layout, so these show the phone layout at full width.

    @Test
    @Config(qualifiers = TABLET_7)
    fun tablet7Today() = todayScreen(TABLET_7_DIR)

    @Test
    @Config(qualifiers = TABLET_7)
    fun tablet7Slot() = slotScreen(TABLET_7_DIR)

    @Test
    @Config(qualifiers = TABLET_7)
    fun tablet7Medications() = medicationsScreen(TABLET_7_DIR)

    @Test
    @Config(qualifiers = TABLET_7)
    fun tablet7History() = historyScreen(TABLET_7_DIR)

    @Test
    @Config(qualifiers = TABLET_10)
    fun tablet10Today() = todayScreen(TABLET_10_DIR)

    @Test
    @Config(qualifiers = TABLET_10)
    fun tablet10Slot() = slotScreen(TABLET_10_DIR)

    @Test
    @Config(qualifiers = TABLET_10)
    fun tablet10Medications() = medicationsScreen(TABLET_10_DIR)

    @Test
    @Config(qualifiers = TABLET_10)
    fun tablet10History() = historyScreen(TABLET_10_DIR)

    private fun todayScreen(dir: String = "") = screen("${dir}01_today", at = LocalTime(12, 40)) { morningTaken() }

    private fun slotScreen(dir: String = "") =
        screen("${dir}03_slot", at = LocalTime(8, 20), launchRequest = LaunchRequest.OpenSlot(TimeSlot.MORNING)) {
            sample().logDose(ReferenceFixtures.metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, millis(date, 8, 5))
        }

    private fun medicationsScreen(dir: String = "") =
        screen("${dir}04_medications", at = LocalTime(12, 40), tab = "medications") { forms() }

    private fun historyScreen(dir: String = "") = screen("${dir}06_history", at = LocalTime(21, 30), tab = "history") { eveningDone() }

    @Test
    @Config(qualifiers = "+w512dp-h250dp-xhdpi")
    fun featureGraphic() {
        val plan = sample().planFor(date)
        composeRule.setContent { FeatureGraphic(plan) }
        composeRule.onRoot().captureRoboImage(output("feature_graphic"), recordOptions)
    }

    @Composable
    private fun FeatureGraphic(plan: DayPlan) {
        TempusTheme(darkTheme = true) {
            val colors = TempusTheme.colors
            val type = TempusTheme.type
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors.brand))) {
                Glow(alpha = 0.16f, Modifier.offset(x = (-90).dp, y = 110.dp).size(300.dp))
                Glow(alpha = 0.12f, Modifier.offset(x = 330.dp, y = (-120).dp).size(280.dp))
                Row(Modifier.fillMaxSize().padding(start = 30.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(222.dp)) {
                        Text("Tempus\nRemedium", style = type.headline.copy(fontSize = 36.sp, lineHeight = 38.sp), color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(tagline, style = type.headlineAccent.copy(fontSize = 23.sp, lineHeight = 27.sp), color = Color(0xFFBFE2FF))
                        Spacer(Modifier.height(16.dp))
                        Text(byline, style = type.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(236.dp), contentAlignment = Alignment.Center) {
                        HojePanel(
                            plan = plan,
                            highlight = TimeSlot.MORNING,
                            modifier =
                                Modifier
                                    .requiredWidth(340.dp)
                                    .graphicsLayer(scaleX = 0.69f, scaleY = 0.69f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Glow(
        alpha: Float,
        modifier: Modifier = Modifier,
    ) {
        Box(modifier.background(Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent)), CircleShape))
    }

    /** The data behind most screens: the reference medications with a month of history. */
    private fun sample(): AppData {
        val start = LocalDate(2026, 9, 1)
        val medications =
            with(ReferenceFixtures) { listOf(metformina, vitaminaD, omega3, magnesio) }
                .map { it.copy(name = names.getValue(it.id), schedule = it.schedule.copy(startDate = start)) }
        val history =
            (1 until HISTORY_DAYS).fold(AppData(medications = medications, settings = Settings(language = language))) { data, offset ->
                val day = date.minus(DatePeriod(days = offset))
                TimeSlot.entries.fold(data) { acc, slot ->
                    val status = if ((offset to slot) in skipped) DoseStatus.SKIPPED else DoseStatus.TAKEN
                    acc.logSlot(day, slot, status, millis(day, slot))
                }
            }
        // Logging a month of doses used the stock up; set what the panels should show.
        return history.copy(medications = history.medications.map { it.copy(stock = stock[it.id]) })
    }

    private fun morningTaken() = sample().logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, millis(date, TimeSlot.MORNING))

    /** [sample] with today's doses up to NOITE taken. */
    private fun eveningDone() =
        TimeSlot.entries.dropLast(1).fold(sample()) { data, slot -> data.logSlot(date, slot, DoseStatus.TAKEN, millis(date, slot)) }

    /** One medication of each kind that is not swallowed, next to Metformina. */
    private fun forms(): AppData {
        fun medication(
            id: String,
            strength: String,
            shape: PillShape,
            color: PillColor,
            doses: Map<TimeSlot, Int>,
            frequency: Frequency,
        ) = Medication(
            id = id,
            name = names.getValue(id),
            strength = strength,
            shape = shape,
            color = color,
            schedule = Schedule(doses = doses, frequency = frequency, startDate = date),
            stock = Stock(remaining = 8),
        )
        val morningEvening = mapOf(TimeSlot.MORNING to 2, TimeSlot.EVENING to 2)
        val inhalerDays = Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), everyWeeks = 2)
        return AppData(
            settings = Settings(language = language),
            medications =
                listOf(
                    ReferenceFixtures.metformina.copy(
                        name = names.getValue(ReferenceFixtures.metformina.id),
                        stock = Stock(remaining = 24),
                    ),
                    medication("salbutamol", "100µg", PillShape.INHALER, PillColor.BLUE, morningEvening, inhalerDays),
                    medication(
                        "nicotina",
                        "14mg/24h",
                        PillShape.PATCH,
                        PillColor.PEACH,
                        mapOf(TimeSlot.MORNING to 1),
                        Frequency.EveryNDays(3),
                    ),
                    medication("mometasona", "50µg", PillShape.NASAL_SPRAY, PillColor.GREEN, morningEvening, Frequency.Daily),
                    medication("latanoprost", "", PillShape.DROPS, PillColor.TEAL, mapOf(TimeSlot.BEDTIME to 1), Frequency.Cycle(21, 7)),
                    medication(
                        "vitamina-b12",
                        "1mg",
                        PillShape.INJECTION,
                        PillColor.PINK,
                        mapOf(TimeSlot.MORNING to 1),
                        Frequency.Monthly(),
                    ),
                ),
        )
    }

    /** The antibiotic course from 08:00 today, with its first [dosesTaken] doses taken on time. */
    private fun AppData.withCourse(dosesTaken: Int): AppData {
        val course = CourseFixtures.amoxicilina.let { it.copy(name = names.getValue(it.id)) }
        return (0 until dosesTaken).fold(upsertMedication(course)) { data, index ->
            data.logCourseDose(course.id, index, DoseStatus.TAKEN, millis(date, 8 + index * 8, 2))
        }
    }

    private fun AppData.withStock(
        id: String,
        stock: Stock,
    ) = copy(medications = medications.map { if (it.id == id) it.copy(stock = stock) else it })

    @OptIn(ExperimentalRoborazziApi::class)
    private fun screen(
        name: String,
        at: LocalTime,
        tab: String? = null,
        scrollToEnd: Boolean = false,
        launchRequest: LaunchRequest? = null,
        data: () -> AppData,
    ) {
        val document = MedicationRepository.json.encodeToString(AppData.serializer(), data())
        val repository = MedicationRepository(InMemoryDocumentStorage(document))
        runBlocking { repository.load() }
        val instant = LocalDateTime(date, at).toInstant(zone)
        val clock =
            object : Clock {
                override fun now(): Instant = instant
            }
        composeRule.setContent {
            val viewModel = remember { AppViewModel(repository, clock) { zone } }
            val platform =
                remember {
                    PlatformUi(settingsExtras = { settings, onChange -> AndroidSettingsExtras(settings, onChange, {}, {}) })
                }
            App(repository = repository, platform = platform, launchRequest = launchRequest, viewModel = viewModel)
        }
        tab?.let { composeRule.onNode(hasText(tabs.getValue(it)) and hasClickAction()).performClick() }
        if (scrollToEnd) composeRule.onNode(hasScrollAction()).performTouchInput { repeat(4) { swipeUp() } }
        composeRule.waitForIdle()
        // The whole screen, so the bottom sheet's window is included.
        captureScreenRoboImage(output(name), recordOptions)
    }

    /**
     * The widgets as the launcher draws them, on a wallpaper in the brand blues, with a status bar,
     * the date and the gesture bar. Robolectric cannot render a launcher, so this is drawn here;
     * the widgets themselves are the real Glance layouts. Light or dark follows the qualifiers.
     */
    private fun homeScreen(
        name: String,
        at: LocalTime,
        data: () -> AppData,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appData = data()
        val hoje = runBlocking { HojeWidgetUi.build(context, appData, date) }
        val course = runBlocking { CourseWidgetUi.build(context, appData, LocalDateTime(date, at)) }
        val repor = runBlocking { ReporWidgetUi.build(context, appData) }
        val widgets =
            listOf(
                renderWidget(context, DpSize(WIDGET_WIDTH_DP.dp, 236.dp)) { HojeWidgetContent(hoje) },
                renderWidget(context, DpSize(WIDGET_WIDTH_DP.dp, 132.dp)) { CourseWidgetContent(course) },
                renderWidget(context, DpSize(WIDGET_WIDTH_DP.dp, 224.dp)) { ReporWidgetContent(repor) },
            )
        val dark = WidgetStyle.palette(context) == TempusPalette.Dark
        deviceScreen(name, dark) { canvas, dp, text ->
            val margin = (canvas.width - WIDGET_WIDTH_DP * dp) / 2
            canvas.drawText(at.toString(), margin + 8 * dp, 30 * dp, text(14f, Typeface.BOLD))
            canvas.drawText(homeDate, margin + 8 * dp, 92 * dp, text(24f, Typeface.NORMAL))
            var y = 124 * dp
            for (widget in widgets) {
                canvas.drawBitmap(widget, margin, y, null)
                y += widget.height + 14 * dp
            }
        }
    }

    /**
     * The HOJE widget alone on a lock screen: padlock, large clock, date, then the widget, on the
     * dark wallpaper. Drawn like [homeScreen]; the widget follows the qualifiers' day/night setting.
     */
    private fun lockScreen(
        name: String,
        at: LocalTime,
        data: () -> AppData,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hoje = runBlocking { HojeWidgetUi.build(context, data(), date) }
        val widget = renderWidget(context, DpSize(WIDGET_WIDTH_DP.dp, 240.dp)) { HojeWidgetContent(hoje) }
        deviceScreen(name, darkWallpaper = true) { canvas, dp, text ->
            val center = canvas.width / 2f
            // Padlock: shackle and body.
            val lock =
                text(0f, Typeface.NORMAL).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2 * dp
                }
            canvas.drawArc(RectF(center - 5 * dp, 30 * dp, center + 5 * dp, 42 * dp), 180f, 180f, false, lock)
            canvas.drawRoundRect(RectF(center - 8 * dp, 37 * dp, center + 8 * dp, 50 * dp), 3 * dp, 3 * dp, text(0f, Typeface.NORMAL))
            val clock =
                text(104f, Typeface.NORMAL).apply {
                    textAlign = Paint.Align.CENTER
                    typeface =
                        Typeface.create("sans-serif-light", Typeface.NORMAL)
                }
            canvas.drawText(at.toString(), center, 182 * dp, clock)
            canvas.drawText(homeDate, center, 222 * dp, text(18f, Typeface.NORMAL).apply { textAlign = Paint.Align.CENTER })
            canvas.drawBitmap(widget, (canvas.width - widget.width) / 2f, 290 * dp, null)
        }
    }

    /**
     * A phone screen drawn by hand: the wallpaper in the brand blues with soft blobs, whatever
     * [content] draws (given the canvas, the density and a text paint factory) and the gesture bar.
     */
    private fun deviceScreen(
        name: String,
        darkWallpaper: Boolean,
        content: (android.graphics.Canvas, Float, (Float, Int) -> Paint) -> Unit,
    ) {
        val metrics = ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics
        val dp = metrics.density
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val (top, bottom, blob) =
            if (darkWallpaper) Triple(0xFF0E2E52, 0xFF050D18, 0x55125A98) else Triple(0xFFE4F0FA, 0xFFB7D3EC, 0x99FFFFFF)
        canvas.drawPaint(
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, width * 0.4f, height.toFloat(), top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
            },
        )
        // Soft blobs, as on mariacf.pt.
        for ((x, y, r) in listOf(Triple(0.15f, 0.2f, 0.6f), Triple(0.9f, 0.75f, 0.55f))) {
            val transparent = blob.toInt() and 0x00FFFFFF
            canvas.drawCircle(
                x * width,
                y * height,
                r * width,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(x * width, y * height, r * width, blob.toInt(), transparent, Shader.TileMode.CLAMP)
                },
            )
        }

        val ink = if (darkWallpaper) android.graphics.Color.WHITE else TempusPalette.Light.ink.toArgb()
        val text = { size: Float, style: Int ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                textSize = size * dp
                typeface = Typeface.create(Typeface.SANS_SERIF, style)
            }
        }
        content(canvas, dp, text)
        val bar = RectF(width / 2 - 54 * dp, height - 14 * dp, width / 2 + 54 * dp, height - 10 * dp)
        canvas.drawRoundRect(bar, 2 * dp, 2 * dp, text(0f, Typeface.NORMAL).apply { alpha = 0xCC })

        val file = File(output(name))
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun millis(
        day: LocalDate,
        hour: Int,
        minute: Int,
    ) = LocalDateTime(day, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()

    private fun millis(
        day: LocalDate,
        slot: TimeSlot,
    ) = millis(day, slot.defaultTime.hour, 5)

    private fun output(name: String) = "../store-listing/source/$locale/$name.png"

    companion object {
        private const val HISTORY_DAYS = 30
        private const val WIDGET_WIDTH_DP = 380

        /** A 7" tablet in landscape (Nexus 7 class) and a 10" one (Pixel Tablet class). */
        const val TABLET_7 = "+w960dp-h600dp-land-xhdpi"
        const val TABLET_10 = "+w1280dp-h800dp-land-xhdpi"
        private const val TABLET_7_DIR = "tablet-7/"
        private const val TABLET_10_DIR = "tablet-10/"

        /** A few missed doses, so the history looks lived in. */
        private val skipped =
            setOf(
                3 to TimeSlot.EVENING,
                9 to TimeSlot.MIDDAY,
                12 to TimeSlot.EVENING,
                12 to TimeSlot.BEDTIME,
                19 to TimeSlot.MORNING,
                26 to TimeSlot.MIDDAY,
            )

        private val stock =
            mapOf(
                ReferenceFixtures.metformina.id to Stock(remaining = 6),
                ReferenceFixtures.vitaminaD.id to Stock(remaining = 24),
                ReferenceFixtures.omega3.id to Stock(remaining = 3),
            )

        /** Always writes the file: these are generated outputs, never compared with goldens. */
        @OptIn(ExperimentalRoborazziApi::class)
        val recordOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
    }
}

@Config(sdk = [36], qualifiers = "pt-rPT-w412dp-h824dp-420dpi")
class PortugueseStoreListingCaptures :
    StoreListingCaptures(
        locale = "pt-PT",
        language = AppLanguage.PT,
        names =
            with(ReferenceFixtures) {
                mapOf(
                    metformina.id to "Metformina",
                    vitaminaD.id to "Vitamina D",
                    omega3.id to "Ómega 3",
                    magnesio.id to "Magnésio",
                    CourseFixtures.amoxicilina.id to "Amoxicilina",
                    "salbutamol" to "Salbutamol",
                    "nicotina" to "Nicotina",
                    "mometasona" to "Mometasona",
                    "latanoprost" to "Latanoprost",
                    "vitamina-b12" to "Vitamina B12",
                )
            },
        tabs = mapOf("medications" to "Medicamentos", "history" to "Histórico", "settings" to "Definições"),
        tagline = "As doses de hoje,\na horas.",
        byline = "por Dra. Maria Cortês Ferreira",
        homeDate = "quinta-feira, 22 de outubro",
    ) {
    /** The launcher icon's two layers at 512 px, cropped to the part a launcher shows (72 of 108 dp). */
    @Test
    @Config(qualifiers = "+w512dp-h512dp-mdpi")
    fun icon() {
        composeRule.setContent {
            val context = LocalContext.current
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { id ->
                        val drawable = requireNotNull(ContextCompat.getDrawable(context, id))
                        val full = size.width * 108f / 72f
                        val inset = ((full - size.width) / 2).toInt()
                        drawable.setBounds(-inset, -inset, size.width.toInt() + inset, size.height.toInt() + inset)
                        drawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("../store-listing/source/icon.png", recordOptions)
    }

    /**
     * Every form, large, in a 3 × 3 grid of 120 dp cells, on white and on black: the video script
     * recovers the transparency from the pair.
     */
    @Test
    @Config(qualifiers = "+w360dp-h360dp-xxhdpi")
    fun formsOnWhite() = formsGrid(Color.White, "forms_white")

    @Test
    @Config(qualifiers = "+w360dp-h360dp-xxhdpi")
    fun formsOnBlack() = formsGrid(Color.Black, "forms_black")

    private fun formsGrid(
        background: Color,
        name: String,
    ) {
        val colors =
            listOf(
                PillColor.TEAL,
                PillColor.PINK,
                PillColor.PEACH,
                PillColor.BLUE,
                PillColor.BLUE,
                PillColor.PEACH,
                PillColor.GREEN,
                PillColor.TEAL,
                PillColor.PINK,
            )
        composeRule.setContent {
            TempusTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize().background(background)) {
                    PillShape.entries.chunked(3).forEachIndexed { row, shapes ->
                        Row {
                            shapes.forEachIndexed { column, shape ->
                                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                    PillIcon(shape, colors[row * 3 + column], height = 72.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("../store-listing/source/$name.png", recordOptions)
    }
}

@Config(sdk = [36], qualifiers = "en-rUS-w412dp-h824dp-420dpi")
class EnglishStoreListingCaptures :
    StoreListingCaptures(
        locale = "en-US",
        language = AppLanguage.EN,
        names =
            with(ReferenceFixtures) {
                mapOf(
                    metformina.id to "Metformin",
                    vitaminaD.id to "Vitamin D",
                    omega3.id to "Omega 3",
                    magnesio.id to "Magnesium",
                    CourseFixtures.amoxicilina.id to "Amoxicillin",
                    "salbutamol" to "Salbutamol",
                    "nicotina" to "Nicotine",
                    "mometasona" to "Mometasone",
                    "latanoprost" to "Latanoprost",
                    "vitamina-b12" to "Vitamin B12",
                )
            },
        tabs = mapOf("medications" to "Medications", "history" to "History", "settings" to "Settings"),
        tagline = "Today's doses,\non time.",
        byline = "by Dr Maria Cortês Ferreira",
        homeDate = "Thursday, October 22",
    )
