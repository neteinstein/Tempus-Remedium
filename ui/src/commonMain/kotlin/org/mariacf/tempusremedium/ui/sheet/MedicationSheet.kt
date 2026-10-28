package org.mariacf.tempusremedium.ui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.plannedLastDose
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.intervalLabel
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.components.scheduleLabel
import org.mariacf.tempusremedium.ui.pill.aspectRatio
import org.mariacf.tempusremedium.ui.pill.drawPill
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.about_author
import org.mariacf.tempusremedium.ui.resources.about_disclaimer
import org.mariacf.tempusremedium.ui.resources.about_site
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.field_notes
import org.mariacf.tempusremedium.ui.resources.pdf_courses
import org.mariacf.tempusremedium.ui.resources.pdf_date_time
import org.mariacf.tempusremedium.ui.resources.pdf_day
import org.mariacf.tempusremedium.ui.resources.pdf_day_body
import org.mariacf.tempusremedium.ui.resources.pdf_each
import org.mariacf.tempusremedium.ui.resources.pdf_first_dose
import org.mariacf.tempusremedium.ui.resources.pdf_how_often
import org.mariacf.tempusremedium.ui.resources.pdf_last_dose
import org.mariacf.tempusremedium.ui.resources.pdf_page
import org.mariacf.tempusremedium.ui.resources.pdf_since
import org.mariacf.tempusremedium.ui.resources.pdf_title
import org.mariacf.tempusremedium.ui.resources.pdf_updated
import org.mariacf.tempusremedium.ui.resources.pdf_when
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.theme.TempusPalette
import org.mariacf.tempusremedium.ui.theme.TempusTypography
import org.mariacf.tempusremedium.ui.theme.frauncesFamily
import org.mariacf.tempusremedium.ui.theme.jakartaFamily

/** A4 in PDF points; pages are drawn in points (1 unit = 1/72 inch). */
const val SHEET_WIDTH = 595f
const val SHEET_HEIGHT = 842f

/** Draws one page of the medications sheet. */
typealias SheetPage = DrawScope.() -> Unit

/*
 * The medications sheet: a PDF to read and print, for the patient, a carer or a doctor. Large
 * type, the pill as it looks, and plain words: first the day hour by hour, then each medication
 * with what it looks like, when and how often to take it, and how many are left.
 */

/** One line of "Quando tomar": "08:00 Manhã · 1 unidade", or "De 8 em 8 horas · 6,5 mL" for a course. */
class SheetDose(
    val time: String,
    val label: String,
    val amount: String,
)

/** A medication as the sheet describes it, in the app's language. */
class SheetMedication(
    val shape: PillShape,
    val color: PillColor,
    val name: String,
    val looks: String,
    val doses: List<SheetDose>,
    /** Label and value rows under the doses: frequency, dates, notes. */
    val details: List<Pair<String, String>>,
    val stock: String?,
    /** For the day overview: the slots and amounts, empty for a course. */
    val slots: Map<TimeSlot, String>,
    /** For the day overview: the frequency when it is not every day. */
    val frequencyNote: String?,
    /** For the day overview, courses only: "De 8 em 8 horas · 6,5 mL". */
    val courseLine: String?,
)

/** Every text of the sheet, resolved in composition so it follows the app's language. */
class SheetTexts(
    val appName: String,
    val title: String,
    val updated: String,
    val day: String,
    val dayBody: String,
    val each: String,
    val courses: String,
    val whenLabel: String,
    val footer: String,
    val disclaimer: String,
    /** "Página {page} de {pages}" with the numbers still to fill in. */
    val pageTemplate: String,
    val slotNames: Map<TimeSlot, String>,
    val slotTimes: Map<TimeSlot, String>,
    val medications: List<SheetMedication>,
) {
    /**
     * Whether every text has loaded. On the web, strings load asynchronously and read as empty
     * on the first composition, so the sheet waits for this before it is drawn.
     */
    val isResolved: Boolean
        get() =
            listOf(
                appName,
                title,
                updated,
                day,
                dayBody,
                each,
                courses,
                whenLabel,
                footer,
                disclaimer,
                pageTemplate,
            ).all { it.isNotEmpty() } &&
                slotNames.values.all { it.isNotEmpty() } &&
                medications.all { medication ->
                    medication.looks.length > 3 &&
                        medication.doses.all { it.label.isNotEmpty() && it.amount.isNotEmpty() } &&
                        medication.details.all { (label, value) -> label.isNotEmpty() && value.isNotEmpty() } &&
                        medication.stock?.isNotEmpty() != false
                }
}

private const val PAGE_PLACEHOLDER = "{page}"
private const val PAGES_PLACEHOLDER = "{pages}"

@Composable
fun sheetTexts(
    medications: List<Medication>,
    settings: Settings,
    today: LocalDate,
): SheetTexts =
    SheetTexts(
        appName = stringResource(Res.string.app_name),
        title = stringResource(Res.string.pdf_title),
        updated = stringResource(Res.string.pdf_updated, longDateLabel(today)),
        day = stringResource(Res.string.pdf_day),
        dayBody = stringResource(Res.string.pdf_day_body),
        each = stringResource(Res.string.pdf_each),
        courses = stringResource(Res.string.pdf_courses),
        whenLabel = stringResource(Res.string.pdf_when),
        footer =
            listOf(
                stringResource(Res.string.app_name),
                stringResource(Res.string.about_author),
                stringResource(Res.string.about_site),
            ).joinToString(" · "),
        disclaimer = stringResource(Res.string.about_disclaimer),
        pageTemplate = stringResource(Res.string.pdf_page, PAGE_PLACEHOLDER, PAGES_PLACEHOLDER),
        slotNames = TimeSlot.entries.associateWith { stringResource(it.label) },
        slotTimes = TimeSlot.entries.associateWith { settings.timeOf(it).hhmm() },
        medications = medications.map { sheetMedication(it, settings) },
    )

@Composable
private fun sheetMedication(
    medication: Medication,
    settings: Settings,
): SheetMedication {
    val course = medication.course
    val details = mutableListOf<Pair<String, String>>()
    val doses: List<SheetDose>
    val slots: Map<TimeSlot, String>
    var courseLine: String? = null
    var frequencyNote: String? = null
    if (course != null) {
        val interval = intervalLabel(course.intervalHours)
        val amount = doseLabel(course.amount, course.unit)
        doses = listOf(SheetDose(time = "", label = interval, amount = amount))
        slots = emptyMap()
        courseLine = "$interval · $amount"
        details += stringResource(Res.string.pdf_first_dose) to dateTime(course.firstDose)
        course.plannedLastDose()?.let { details += stringResource(Res.string.pdf_last_dose) to dateTime(it) }
    } else {
        val schedule = medication.schedule
        slots =
            TimeSlot.entries
                .mapNotNull { slot -> schedule.doses[slot]?.takeIf { it > 0 }?.let { slot to doseLabel(medication, it) } }
                .toMap()
        doses = slots.map { (slot, amount) -> SheetDose(settings.timeOf(slot).hhmm(), stringResource(slot.label), amount) }
        val howOften = scheduleLabel(schedule, fullDays = true)
        details += stringResource(Res.string.pdf_how_often) to howOften
        details += stringResource(Res.string.pdf_since) to longDateLabel(schedule.startDate)
        if (schedule.frequency != Frequency.Daily || schedule.occurrences != null || schedule.endDate != null) frequencyNote = howOften
    }
    if (medication.notes.isNotBlank()) details += stringResource(Res.string.field_notes) to medication.notes.trim()
    val looks = stringResource(medication.shape.label) + " · " + stringResource(medication.color.label)
    val tablets = medication.stockTablets
    return SheetMedication(
        shape = medication.shape,
        color = medication.color,
        name = medication.displayName,
        looks = looks,
        doses = doses,
        details = details,
        stock = tablets?.let { pluralStringResource(Res.plurals.units_left, it, it) },
        slots = slots,
        frequencyNote = frequencyNote,
        courseLine = courseLine,
    )
}

@Composable
private fun dateTime(at: LocalDateTime): String = stringResource(Res.string.pdf_date_time, longDateLabel(at.date), at.time.hhmm())

/** The sheet's colours and fonts: always the light palette, since it is meant for paper. */
@Composable
fun sheetStyle(): SheetStyle = SheetStyle(TempusPalette.Light, TempusTypography(frauncesFamily(), jakartaFamily()))

class SheetStyle(
    val colors: TempusPalette,
    val type: TempusTypography,
)

private const val MARGIN = 44f
private const val CONTENT_WIDTH = SHEET_WIDTH - 2 * MARGIN
private const val FOOTER_TOP = SHEET_HEIGHT - 58f
private const val FIRST_HEADER_HEIGHT = 150f
private const val NEXT_HEADER_HEIGHT = 64f

/** Something drawn at a height on a page; [keepWithNext] keeps a title with what follows it. */
private class Block(
    val height: Float,
    val gapBefore: Float,
    val keepWithNext: Boolean = false,
    val draw: DrawScope.(top: Float) -> Unit,
)

/** Lays [texts] out on A4 pages. */
fun sheetPages(
    texts: SheetTexts,
    style: SheetStyle,
    measurer: TextMeasurer,
): List<SheetPage> {
    val ink = SheetInk(measurer, style)
    val blocks = buildList { sheetBlocks(texts, ink) }
    val pages = mutableListOf<MutableList<Pair<Block, Float>>>(mutableListOf())
    var y = FIRST_HEADER_HEIGHT + 24f
    blocks.forEachIndexed { index, block ->
        val next = blocks.getOrNull(index + 1)
        val needed = block.gapBefore + block.height + if (block.keepWithNext && next != null) next.gapBefore + next.height else 0f
        if (y + needed > FOOTER_TOP - 16f && pages.last().isNotEmpty()) {
            pages += mutableListOf<Pair<Block, Float>>()
            y = NEXT_HEADER_HEIGHT + 20f
        } else if (pages.last().isNotEmpty()) {
            y += block.gapBefore
        }
        pages.last() += block to y
        y += block.height
    }
    return pages.mapIndexed { index, placed ->
        {
            drawRect(Color.White)
            if (index == 0) ink.firstHeader(this, texts) else ink.nextHeader(this, texts)
            placed.forEach { (block, top) -> block.draw(this, top) }
            val page =
                texts.pageTemplate
                    .replace(PAGE_PLACEHOLDER, (index + 1).toString())
                    .replace(PAGES_PLACEHOLDER, pages.size.toString())
            ink.footer(this, texts.footer, page)
        }
    }
}

private fun MutableList<Block>.sheetBlocks(
    texts: SheetTexts,
    ink: SheetInk,
) {
    val medications = texts.medications
    val bySlot = TimeSlot.entries.associateWith { slot -> medications.filter { slot in it.slots } }.filterValues { it.isNotEmpty() }
    val courses = medications.filter { it.courseLine != null }
    if (bySlot.isNotEmpty() || courses.isNotEmpty()) {
        add(ink.sectionTitle(texts.day, texts.dayBody, gapBefore = 0f))
        bySlot.forEach { (slot, items) ->
            add(
                ink.dayRow(
                    time = texts.slotTimes.getValue(slot),
                    label = texts.slotNames.getValue(slot),
                    items = items.map { DayItem(it.shape, it.color, it.slots.getValue(slot), it.name, it.frequencyNote) },
                ),
            )
        }
        if (courses.isNotEmpty()) {
            add(
                ink.dayRow(
                    time = null,
                    label = texts.courses,
                    items = courses.map { DayItem(it.shape, it.color, it.courseLine.orEmpty(), it.name, null) },
                ),
            )
        }
    }
    add(ink.sectionTitle(texts.each, null, gapBefore = 30f))
    medications.forEach { add(ink.medicationCard(it, texts)) }
    add(ink.note(texts.disclaimer))
}

private class DayItem(
    val shape: PillShape,
    val color: PillColor,
    val amount: String,
    val name: String,
    val note: String?,
)

/** Measures and draws the parts of the sheet, in points. */
private class SheetInk(
    private val measurer: TextMeasurer,
    style: SheetStyle,
) {
    private val colors = style.colors
    private val type = style.type
    private val display = type.title.copy(fontSize = 30.sp, lineHeight = 34.sp, color = Color.White)
    private val sectionStyle = type.title.copy(fontSize = 21.sp, lineHeight = 26.sp, color = colors.ink)
    private val nameStyle = type.title.copy(fontSize = 20.sp, lineHeight = 25.sp, color = colors.ink)
    private val timeStyle = type.title.copy(fontSize = 22.sp, lineHeight = 26.sp, color = colors.accent)
    private val eyebrowStyle = type.eyebrow.copy(fontSize = 8.5.sp, letterSpacing = 1.3.sp, color = colors.inkMute)
    private val bodyStyle = type.bodyText.copy(fontSize = 12.5.sp, lineHeight = 17.sp, color = colors.ink)
    private val bodyStrong = bodyStyle.copy(fontWeight = FontWeight.Bold)
    private val mutedStyle = type.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp, color = colors.inkMute)
    private val badgeStyle = type.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.stock)
    private val cardShape = CornerRadius(16f, 16f)

    fun measure(
        text: String,
        style: TextStyle,
        width: Float = CONTENT_WIDTH,
    ): TextLayoutResult = measurer.measure(text, style, constraints = Constraints(maxWidth = width.toInt()), density = UNIT_DENSITY)

    fun measure(
        text: AnnotatedString,
        style: TextStyle,
        width: Float,
    ): TextLayoutResult = measurer.measure(text, style, constraints = Constraints(maxWidth = width.toInt()), density = UNIT_DENSITY)

    fun firstHeader(
        scope: DrawScope,
        texts: SheetTexts,
    ) = with(scope) {
        drawRect(
            Brush.linearGradient(colors.brand, start = Offset.Zero, end = Offset(SHEET_WIDTH, FIRST_HEADER_HEIGHT)),
            size = Size(SHEET_WIDTH, FIRST_HEADER_HEIGHT),
        )
        // Soft light blobs, as on the author's site.
        drawCircle(Color.White.copy(alpha = 0.08f), radius = 120f, center = Offset(SHEET_WIDTH - 40f, 10f))
        drawCircle(Color.White.copy(alpha = 0.06f), radius = 70f, center = Offset(SHEET_WIDTH - 150f, FIRST_HEADER_HEIGHT + 10f))
        val eyebrow = measure(texts.appName.uppercase(), eyebrowStyle.copy(color = Color.White.copy(alpha = 0.85f)))
        drawText(eyebrow, topLeft = Offset(MARGIN, 40f))
        val title = measure(texts.title, display, CONTENT_WIDTH - 90f)
        drawText(title, topLeft = Offset(MARGIN, 56f))
        val updated = measure(texts.updated, bodyStyle.copy(color = Color.White.copy(alpha = 0.9f)), CONTENT_WIDTH - 90f)
        drawText(updated, topLeft = Offset(MARGIN, 56f + title.size.height + 6f))
        // The first pills of the sheet, on frosted discs.
        texts.medications.take(3).forEachIndexed { index, medication ->
            val center = Offset(SHEET_WIDTH - MARGIN - 26f - index * 40f, FIRST_HEADER_HEIGHT - 44f - (index % 2) * 18f)
            drawCircle(Color.White.copy(alpha = 0.92f), radius = 24f, center = center)
            pill(medication.shape, medication.color, center, 22f)
        }
    }

    fun nextHeader(
        scope: DrawScope,
        texts: SheetTexts,
    ) = with(scope) {
        drawRect(Brush.linearGradient(colors.brand), size = Size(SHEET_WIDTH, 6f))
        val eyebrow = measure(texts.appName.uppercase(), eyebrowStyle)
        drawText(eyebrow, topLeft = Offset(MARGIN, 26f))
        val title = measure(texts.title, sectionStyle.copy(fontSize = 15.sp, lineHeight = 19.sp))
        drawText(title, topLeft = Offset(MARGIN, 38f))
    }

    fun footer(
        scope: DrawScope,
        credit: String,
        page: String,
    ) = with(scope) {
        drawLine(colors.line, Offset(MARGIN, FOOTER_TOP), Offset(SHEET_WIDTH - MARGIN, FOOTER_TOP), strokeWidth = 0.8f)
        val pageText = measure(page, mutedStyle)
        drawText(pageText, topLeft = Offset(SHEET_WIDTH - MARGIN - pageText.size.width, FOOTER_TOP + 12f))
        val creditText = measure(credit, mutedStyle, CONTENT_WIDTH - pageText.size.width - 16f)
        drawText(creditText, topLeft = Offset(MARGIN, FOOTER_TOP + 12f))
    }

    fun sectionTitle(
        title: String,
        body: String?,
        gapBefore: Float,
    ): Block {
        val titleText = measure(title, sectionStyle)
        val bodyText = body?.let { measure(it, mutedStyle.copy(fontSize = 11.5.sp, lineHeight = 16.sp)) }
        val height = titleText.size.height + (bodyText?.let { it.size.height + 4f } ?: 0f) + 8f
        return Block(height, gapBefore, keepWithNext = true) { top ->
            drawCircle(colors.dot, radius = 3.5f, center = Offset(MARGIN + 3.5f, top + titleText.size.height / 2f + 1f))
            drawText(titleText, topLeft = Offset(MARGIN + 14f, top))
            bodyText?.let { drawText(it, topLeft = Offset(MARGIN + 14f, top + titleText.size.height + 4f)) }
        }
    }

    /** A time of day with what to take then: "08:00 MANHÃ │ (pill) 1 unidade Metformina 500mg". */
    fun dayRow(
        time: String?,
        label: String,
        items: List<DayItem>,
    ): Block {
        val padding = 14f
        val leftWidth = 104f
        val iconBox = 28f
        val textWidth = CONTENT_WIDTH - 2 * padding - leftWidth - iconBox - 10f
        val timeText = time?.let { measure(it, timeStyle, leftWidth) }
        val labelText = measure(label.uppercase(), eyebrowStyle.copy(fontSize = 9.sp), leftWidth - 8f)
        val leftHeight = (timeText?.size?.height?.plus(2f) ?: 0f) + labelText.size.height
        val lines =
            items.map { item ->
                val line =
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(item.amount) }
                        append("  ·  ")
                        append(item.name)
                    }
                measure(line, bodyStyle.copy(fontSize = 13.sp, lineHeight = 18.sp), textWidth) to
                    item.note?.let { measure(it, mutedStyle, textWidth) }
            }
        val lineHeights = lines.map { (line, note) -> maxOf(iconBox, (line.size.height + (note?.size?.height ?: 0)).toFloat()) }
        val itemsHeight = lineHeights.sum() + (lines.size - 1) * 8f
        val height = maxOf(leftHeight, itemsHeight) + 2 * padding
        return Block(height, gapBefore = 8f) { top ->
            drawRoundRect(colors.background2, topLeft = Offset(MARGIN, top), size = Size(CONTENT_WIDTH, height), cornerRadius = cardShape)
            drawRoundRect(
                colors.accentSoft,
                topLeft = Offset(MARGIN, top),
                size = Size(leftWidth, height),
                cornerRadius = cardShape,
            )
            drawRect(colors.accentSoft, topLeft = Offset(MARGIN + leftWidth - 16f, top), size = Size(16f, height))
            val leftTop = top + (height - leftHeight) / 2f
            timeText?.let { drawText(it, topLeft = Offset(MARGIN + padding, leftTop)) }
            drawText(labelText, topLeft = Offset(MARGIN + padding, leftTop + (timeText?.size?.height?.plus(2f) ?: 0f)))
            var y = top + padding + (height - 2 * padding - itemsHeight) / 2f
            val x = MARGIN + leftWidth + padding
            items.forEachIndexed { index, item ->
                val (line, note) = lines[index]
                val rowHeight = lineHeights[index]
                pill(item.shape, item.color, Offset(x + iconBox / 2f, y + minOf(rowHeight, iconBox) / 2f), 22f)
                val textTop = y + if (note == null) (rowHeight - line.size.height) / 2f else 0f
                drawText(line, topLeft = Offset(x + iconBox + 10f, textTop))
                note?.let { drawText(it, topLeft = Offset(x + iconBox + 10f, textTop + line.size.height)) }
                y += rowHeight + 8f
            }
        }
    }

    fun medicationCard(
        medication: SheetMedication,
        texts: SheetTexts,
    ): Block {
        val padding = 18f
        val disc = 58f
        val innerWidth = CONTENT_WIDTH - 2 * padding
        val headWidth = innerWidth - disc - 14f
        val badge = medication.stock?.let { measure(it, badgeStyle) }
        val nameText = measure(medication.name, nameStyle, headWidth - (badge?.let { it.size.width + 28f } ?: 0f))
        val looksText = measure(medication.looks, mutedStyle.copy(fontSize = 11.5.sp, lineHeight = 16.sp), headWidth)
        val headHeight = maxOf(disc, nameText.size.height + 2f + looksText.size.height)
        val labelWidth = 128f
        val valueWidth = innerWidth - labelWidth
        val whenLabel = measure(texts.whenLabel.uppercase(), eyebrowStyle, labelWidth - 10f)
        val doseLines =
            medication.doses.map { dose ->
                val line =
                    buildAnnotatedString {
                        if (dose.time.isNotEmpty()) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.accent)) { append(dose.time) }
                            append("  ")
                        }
                        append(dose.label)
                        append("  ·  ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(dose.amount) }
                    }
                measure(line, bodyStyle.copy(fontSize = 13.5.sp, lineHeight = 19.sp), valueWidth)
            }
        val details =
            medication.details
                .map { (label, value) ->
                    measure(label.uppercase(), eyebrowStyle, labelWidth - 10f) to measure(value, bodyStyle, valueWidth)
                }
        val dosesHeight = maxOf(whenLabel.size.height, doseLines.sumOf { it.size.height } + (doseLines.size - 1) * 4)
        val detailsHeight = details.sumOf { (label, value) -> maxOf(label.size.height, value.size.height) + 10 }
        val height = padding + headHeight + 16f + 1f + 14f + dosesHeight + 12f + detailsHeight + padding - 10f
        return Block(height, gapBefore = 12f) { top ->
            drawRoundRect(Color.White, topLeft = Offset(MARGIN, top), size = Size(CONTENT_WIDTH, height), cornerRadius = cardShape)
            drawRoundRect(
                colors.line,
                topLeft = Offset(MARGIN, top),
                size = Size(CONTENT_WIDTH, height),
                cornerRadius = cardShape,
                style = Stroke(width = 1f),
            )
            drawRoundRect(
                Brush.linearGradient(colors.brand),
                topLeft = Offset(MARGIN, top + 18f),
                size = Size(4f, 30f),
                cornerRadius = CornerRadius(2f),
            )
            val left = MARGIN + padding
            var y = top + padding
            val discCenter = Offset(left + disc / 2f, y + headHeight / 2f)
            drawCircle(colors.accentSoft, radius = disc / 2f, center = discCenter)
            pill(medication.shape, medication.color, discCenter, 36f)
            val headX = left + disc + 14f
            val headTop = y + (headHeight - nameText.size.height - 2f - looksText.size.height) / 2f
            drawText(nameText, topLeft = Offset(headX, headTop))
            drawText(looksText, topLeft = Offset(headX, headTop + nameText.size.height + 2f))
            badge?.let {
                val width = it.size.width + 20f
                val badgeTop = headTop + (nameText.size.height - it.size.height - 8f) / 2f
                val badgeLeft = MARGIN + CONTENT_WIDTH - padding - width
                drawRoundRect(colors.stockSoft, Offset(badgeLeft, badgeTop), Size(width, it.size.height + 8f), CornerRadius(50f))
                drawText(it, topLeft = Offset(badgeLeft + 10f, badgeTop + 4f))
            }
            y += headHeight + 16f
            drawLine(colors.line, Offset(left, y), Offset(left + innerWidth, y), strokeWidth = 1f)
            y += 14f
            drawText(whenLabel, topLeft = Offset(left, y + 4f))
            var lineY = y
            doseLines.forEach {
                drawText(it, topLeft = Offset(left + labelWidth, lineY))
                lineY += it.size.height + 4f
            }
            y += dosesHeight + 12f
            details.forEach { (label, value) ->
                drawText(label, topLeft = Offset(left, y + 3f))
                drawText(value, topLeft = Offset(left + labelWidth, y))
                y += maxOf(label.size.height, value.size.height) + 10f
            }
        }
    }

    fun note(text: String): Block {
        val layout = measure(text, mutedStyle)
        return Block(layout.size.height.toFloat(), gapBefore = 22f) { top -> drawText(layout, topLeft = Offset(MARGIN, top)) }
    }

    private fun DrawScope.pill(
        shape: PillShape,
        color: PillColor,
        center: Offset,
        height: Float,
    ) {
        val width = height * shape.aspectRatio
        val box = if (width > height * 1.25f) Size(height * 1.25f, height * 1.25f / shape.aspectRatio) else Size(width, height)
        drawPill(shape, color, topLeft = Offset(center.x - box.width / 2f, center.y - box.height / 2f), size = box)
    }

    private companion object {
        val UNIT_DENSITY = Density(1f)
    }
}
