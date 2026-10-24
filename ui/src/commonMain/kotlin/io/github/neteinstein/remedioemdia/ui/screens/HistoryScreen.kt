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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.schedule.AdherenceReport
import io.github.neteinstein.remedioemdia.ui.components.GroupCard
import io.github.neteinstein.remedioemdia.ui.components.GroupDivider
import io.github.neteinstein.remedioemdia.ui.components.ScreenHeader
import io.github.neteinstein.remedioemdia.ui.components.dayLabel
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.history_day
import io.github.neteinstein.remedioemdia.ui.resources.history_no_data
import io.github.neteinstein.remedioemdia.ui.resources.history_rate_caption
import io.github.neteinstein.remedioemdia.ui.resources.history_title
import io.github.neteinstein.remedioemdia.ui.resources.nav_history
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    report: AdherenceReport,
    modifier: Modifier = Modifier,
) {
    val palette = RemedioTheme.colors
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
                        Text(stringResource(Res.string.history_no_data), style = RemedioTheme.type.bodyText, color = palette.inkSoft)
                    } else {
                        // "38%" in Fraunces italic, like the figures on mariacf.pt.
                        Text("${(rate * 100).roundToInt()}%", style = RemedioTheme.type.stat, color = palette.accent)
                        Text(stringResource(Res.string.history_rate_caption), style = RemedioTheme.type.bodySmall, color = palette.inkSoft)
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
                                Text(dayLabel(day.date), style = RemedioTheme.type.rowTitle, color = palette.ink)
                                Text(
                                    stringResource(Res.string.history_day, day.taken, day.due),
                                    style = RemedioTheme.type.bodySmall,
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
