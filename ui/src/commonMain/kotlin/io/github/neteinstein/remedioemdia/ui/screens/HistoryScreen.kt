package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.schedule.AdherenceReport
import io.github.neteinstein.remedioemdia.ui.components.SectionHeader
import io.github.neteinstein.remedioemdia.ui.components.dayLabel
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.history_day
import io.github.neteinstein.remedioemdia.ui.resources.history_no_data
import io.github.neteinstein.remedioemdia.ui.resources.history_rate
import io.github.neteinstein.remedioemdia.ui.resources.history_title
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    report: AdherenceReport,
    modifier: Modifier = Modifier,
) {
    val rate = report.rate
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(title = stringResource(Res.string.history_title), meta = "")
        }
        if (rate != null) {
            item {
                Text(
                    stringResource(Res.string.history_rate, (rate * 100).roundToInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    color = RemedioColors.Amber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
        if (rate == null) {
            item { Text(stringResource(Res.string.history_no_data), color = RemedioColors.TextSecondary) }
        }
        items(report.days.filter { it.due > 0 }, key = { it.date.toString() }) { day ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(120.dp)) {
                    Text(dayLabel(day.date), style = MaterialTheme.typography.titleSmall, color = RemedioColors.TextPrimary)
                    Text(
                        stringResource(Res.string.history_day, day.taken, day.due),
                        style = MaterialTheme.typography.bodySmall,
                        color = RemedioColors.TextSecondary,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(RemedioColors.GlassFill),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(day.taken.toFloat() / day.due)
                            .background(RemedioColors.Amber),
                    )
                }
            }
        }
    }
}
