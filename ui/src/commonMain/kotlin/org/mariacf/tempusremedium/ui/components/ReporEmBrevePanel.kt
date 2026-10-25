package org.mariacf.tempusremedium.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.core.schedule.LowStockItem
import org.mariacf.tempusremedium.ui.pill.StockIcon
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.medications_count
import org.mariacf.tempusremedium.ui.resources.refill_soon
import org.mariacf.tempusremedium.ui.resources.stock_running_out
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * The REPOR EM BREVE panel of docs/design/reference/repor-em-breve.png. [display] switches
 * the jars of the reference for blister packs. Medications in [alerts] (ids of the ones running
 * out today or tomorrow) blink red until tapped, a product addition.
 */
@Composable
fun ReporEmBrevePanel(
    items: List<LowStockItem>,
    modifier: Modifier = Modifier,
    display: StockDisplay = StockDisplay.JARS,
    alerts: Set<String> = emptySet(),
    onItemClick: ((LowStockItem) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(
            title = stringResource(Res.string.refill_soon),
            meta = pluralStringResource(Res.plurals.medications_count, items.size, items.size),
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(TempusTheme.colors.line))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        StockCard(
                            item = item,
                            display = display,
                            alert = item.medication.id in alerts,
                            onClick = onItemClick?.let { { it(item) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StockCard(
    item: LowStockItem,
    display: StockDisplay,
    alert: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val count = pluralStringResource(Res.plurals.units_left, item.remaining, item.remaining)
    val runningOut = stringResource(Res.string.stock_running_out)
    val description = listOfNotNull(item.medication.name, count, runningOut.takeIf { alert }).joinToString(", ")
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (alert) AlertGlow(Modifier.matchParentSize())
            StockIcon(item.medication.shape, item.medication.color, item.remaining, height = 122.dp, display = display)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.medication.name, style = TempusTheme.type.jarName, color = TempusTheme.colors.ink, maxLines = 1)
        Text(count, style = TempusTheme.type.jarCount, color = TempusTheme.colors.stock)
    }
}

/** A red halo that pulses around a jar or blister that runs out today or tomorrow. */
@Composable
private fun AlertGlow(modifier: Modifier = Modifier) {
    val danger = TempusTheme.colors.danger
    val pulse by rememberInfiniteTransition()
        .animateFloat(0.15f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse))
    Box(
        modifier.drawBehind {
            // Reaches a little past the drawing so it frames it without moving the layout; the
            // card's clip leaves 4dp above and below.
            val dx = 10.dp.toPx()
            val dy = 4.dp.toPx()
            val topLeft = Offset(-dx, -dy)
            val size = Size(size.width + 2 * dx, size.height + 2 * dy)
            val radius = CornerRadius(18.dp.toPx())
            drawRoundRect(danger.copy(alpha = 0.16f * pulse), topLeft, size, radius)
            drawRoundRect(danger.copy(alpha = pulse), topLeft, size, radius, style = Stroke(2.dp.toPx()))
        },
    )
}
