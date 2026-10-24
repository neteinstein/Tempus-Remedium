package io.github.neteinstein.remedioemdia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.DayPlan
import io.github.neteinstein.remedioemdia.core.schedule.PlannedDose
import io.github.neteinstein.remedioemdia.core.schedule.SlotPlan
import io.github.neteinstein.remedioemdia.ui.pill.PillIcon
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.doses
import io.github.neteinstein.remedioemdia.ui.resources.today
import io.github.neteinstein.remedioemdia.ui.resources.today_meta
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Taken or skipped pills fade out; the reference only shows pending doses. */
private const val LOGGED_ALPHA = 0.3f

/** Slots show at most this many pills; the rest are summarised as "+N". */
private const val MAX_PILLS_PER_SLOT = 6

/**
 * The HOJE panel of docs/design/reference/hoje.png. [highlight] outlines the slot that needs
 * attention next (a product addition; null renders exactly the reference).
 */
@Composable
fun HojePanel(
    plan: DayPlan,
    modifier: Modifier = Modifier,
    highlight: TimeSlot? = null,
    onSlotClick: ((TimeSlot) -> Unit)? = null,
) {
    val palette = RemedioTheme.colors
    Column(modifier) {
        SectionHeader(
            title = stringResource(Res.string.today),
            meta =
                stringResource(
                    Res.string.today_meta,
                    dayLabel(plan.date),
                    pluralStringResource(Res.plurals.doses, plan.doseCount, plan.doseCount),
                ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(22.dp), palette, elevation = 10.dp)
                .padding(horizontal = 9.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            plan.slots.forEach { slot ->
                SlotCard(
                    slot = slot,
                    highlighted = slot.slot == highlight,
                    onClick = onSlotClick?.let { { it(slot.slot) } },
                    modifier = Modifier.weight(1f).height(129.dp),
                )
            }
        }
        if (plan.legend.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Legend(plan.legend, Modifier.padding(start = 10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotCard(
    slot: SlotPlan,
    highlighted: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = RemedioTheme.colors
    val type = RemedioTheme.type
    val shape = RoundedCornerShape(18.dp)
    val label = stringResource(slot.slot.label)
    val description = listOf(label, slot.time.hhmm()) + slot.doses.map { it.medication.displayName }
    Column(
        modifier
            .clip(shape)
            .glass(shape, palette, strong = true)
            .then(if (highlighted) Modifier.border(1.5.dp, palette.accent, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description.joinToString(", ") }
            .padding(top = 10.dp, bottom = 9.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label.uppercase(),
            style = type.slotLabel,
            color = if (highlighted) palette.accent else palette.inkSoft,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Text(
            slot.time.hhmm(),
            style = type.slotTime,
            color = palette.ink,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        val visible = slot.doses.take(MAX_PILLS_PER_SLOT)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        ) {
            visible.forEach { dose -> SlotPill(dose) }
        }
        if (slot.doses.size > visible.size) {
            Text("+${slot.doses.size - visible.size}", style = type.slotLabel, color = palette.inkMute)
        }
    }
}

@Composable
private fun SlotPill(dose: PlannedDose) {
    val alpha by animateFloatAsState(if (dose.status == null) 1f else LOGGED_ALPHA)
    PillIcon(shape = dose.medication.shape, color = dose.medication.color, height = 24.dp, alpha = alpha)
}

/** Two equal columns, filled row by row. */
@Composable
private fun Legend(
    medications: List<Medication>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        medications.chunked(2).forEach { row ->
            Row {
                row.forEach { LegendEntry(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendEntry(
    medication: Medication,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PillIcon(medication.shape, medication.color, height = 13.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            medication.displayName,
            style = RemedioTheme.type.legend,
            color = RemedioTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
