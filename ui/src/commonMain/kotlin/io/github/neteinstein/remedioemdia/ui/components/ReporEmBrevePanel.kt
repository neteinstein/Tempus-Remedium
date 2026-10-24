package io.github.neteinstein.remedioemdia.ui.components

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
import io.github.neteinstein.remedioemdia.core.schedule.LowStockItem
import io.github.neteinstein.remedioemdia.ui.pill.Jar
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.medications_count
import io.github.neteinstein.remedioemdia.ui.resources.refill_soon
import io.github.neteinstein.remedioemdia.ui.resources.units_left
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioType
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The REPOR EM BREVE panel of docs/design/reference/repor-em-breve.png. */
@Composable
fun ReporEmBrevePanel(
    items: List<LowStockItem>,
    modifier: Modifier = Modifier,
    onItemClick: ((LowStockItem) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(
            title = stringResource(Res.string.refill_soon),
            meta = pluralStringResource(Res.plurals.medications_count, items.size, items.size),
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(RemedioColors.Divider))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        JarCard(
                            item = item,
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
private fun JarCard(
    item: LowStockItem,
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
        Jar(item.medication.shape, item.medication.color, item.remaining, height = 122.dp)
        Spacer(Modifier.height(8.dp))
        Text(item.medication.name, style = RemedioType.JarName, color = RemedioColors.TextPrimary, maxLines = 1)
        Text(count, style = RemedioType.JarCount, color = RemedioColors.Amber)
    }
}
