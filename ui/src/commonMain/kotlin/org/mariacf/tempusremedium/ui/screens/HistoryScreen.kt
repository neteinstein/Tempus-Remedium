package org.mariacf.tempusremedium.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.schedule.AdherenceReport
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.dayLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.history_day
import org.mariacf.tempusremedium.ui.resources.history_no_data
import org.mariacf.tempusremedium.ui.resources.history_rate_caption
import org.mariacf.tempusremedium.ui.resources.history_title
import org.mariacf.tempusremedium.ui.resources.nav_history
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    report: AdherenceReport,
    modifier: Modifier = Modifier,
) {
    val palette = TempusTheme.colors
    val rate = report.rate
    val days = report.days.filter { it.due > 0 }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(eyebrow = stringResource(Res.string.history_title), title = stringResource(Res.string.nav_history))
        }
        item {
            GroupCard {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    if (rate == null) {
                        Text(stringResource(Res.string.history_no_data), style = TempusTheme.type.bodyText, color = palette.inkSoft)
                    } else {
                        // "38%" in Fraunces italic, like the figures on mariacf.pt.
                        Text("${(rate * 100).roundToInt()}%", style = TempusTheme.type.stat, color = palette.accent)
                        Text(stringResource(Res.string.history_rate_caption), style = TempusTheme.type.bodySmall, color = palette.inkSoft)
                    }
                }
            }
        }
        if (days.isNotEmpty()) {
            item {
                GroupCard {
                    days.forEachIndexed { index, day ->
                        if (index > 0) GroupDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.width(118.dp)) {
                                Text(dayLabel(day.date), style = TempusTheme.type.rowTitle, color = palette.ink)
                                Text(
                                    stringResource(Res.string.history_day, day.taken, day.due),
                                    style = TempusTheme.type.bodySmall,
                                    color = palette.inkMute,
                                )
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(palette.line),
                            ) {
                                if (day.taken > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(day.taken.toFloat() / day.due)
                                            .clip(CircleShape)
                                            .background(palette.brandBrush),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
