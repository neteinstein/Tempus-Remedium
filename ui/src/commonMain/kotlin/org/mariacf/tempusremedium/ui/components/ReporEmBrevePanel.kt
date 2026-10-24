package org.mariacf.tempusremedium.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * The REPOR EM BREVE panel of docs/design/reference/repor-em-breve.png. [display] switches
 * the jars of the reference for blister packs.
 */
@Composable
fun ReporEmBrevePanel(
    items: List<LowStockItem>,
    modifier: Modifier = Modifier,
    display: StockDisplay = StockDisplay.JARS,
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
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val count = pluralStringResource(Res.plurals.units_left, item.remaining, item.remaining)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "${item.medication.name}, $count" }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StockIcon(item.medication.shape, item.medication.color, item.remaining, height = 122.dp, display = display)
        Spacer(Modifier.height(8.dp))
        Text(item.medication.name, style = TempusTheme.type.jarName, color = TempusTheme.colors.ink, maxLines = 1)
        Text(count, style = TempusTheme.type.jarCount, color = TempusTheme.colors.stock)
    }
}
