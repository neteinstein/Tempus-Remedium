package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_back
import org.mariacf.tempusremedium.ui.resources.widget_banner
import org.mariacf.tempusremedium.ui.resources.widget_banner_close
import org.mariacf.tempusremedium.ui.resources.widget_guide_add
import org.mariacf.tempusremedium.ui.resources.widget_guide_eyebrow
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_1
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_2
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_3
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_4
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_5
import org.mariacf.tempusremedium.ui.resources.widget_guide_home_title
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_1
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_2
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_3
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_4
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_intro
import org.mariacf.tempusremedium.ui.resources.widget_guide_lock_title
import org.mariacf.tempusremedium.ui.resources.widget_guide_privacy
import org.mariacf.tempusremedium.ui.resources.widget_guide_title
import org.mariacf.tempusremedium.ui.resources.widget_guide_what_body
import org.mariacf.tempusremedium.ui.resources.widget_guide_what_title
import org.mariacf.tempusremedium.ui.resources.widget_guide_why_1
import org.mariacf.tempusremedium.ui.resources.widget_guide_why_2
import org.mariacf.tempusremedium.ui.resources.widget_guide_why_3
import org.mariacf.tempusremedium.ui.resources.widget_guide_why_title
import org.mariacf.tempusremedium.ui.theme.TempusTheme

private val HomeSteps =
    listOf(
        Res.string.widget_guide_home_1,
        Res.string.widget_guide_home_2,
        Res.string.widget_guide_home_3,
        Res.string.widget_guide_home_4,
        Res.string.widget_guide_home_5,
    )

private val LockSteps =
    listOf(
        Res.string.widget_guide_lock_1,
        Res.string.widget_guide_lock_2,
        Res.string.widget_guide_lock_3,
        Res.string.widget_guide_lock_4,
    )

private val Reasons = listOf(Res.string.widget_guide_why_1, Res.string.widget_guide_why_2, Res.string.widget_guide_why_3)

/** "Add the widget…": the bar above the app while no widget is on a home or lock screen. */
@Composable
fun WidgetBanner(
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TempusTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(palette.brandBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.widget_banner),
            style = TempusTheme.type.button,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.widget_banner_close), tint = Color.White)
        }
    }
}

/** What a widget is, why to add one, and how, step by step. [onAdd] is null when the launcher cannot pin one. */
@Composable
fun WidgetGuideScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)
    val palette = TempusTheme.colors
    val type = TempusTheme.type
    Column(
        modifier
            .fillMaxSize()
            .brandBackground(palette)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back), tint = palette.ink)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(stringResource(Res.string.widget_guide_eyebrow), stringResource(Res.string.widget_guide_title))

            Section(Res.string.widget_guide_what_title) {
                Text(stringResource(Res.string.widget_guide_what_body), style = type.bodyText, color = palette.inkSoft)
            }

            Section(Res.string.widget_guide_why_title) {
                GroupCard {
                    Reasons.forEachIndexed { index, reason ->
                        if (index > 0) GroupDivider()
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(palette.dot, CircleShape))
                            Spacer(Modifier.width(14.dp))
                            Text(stringResource(reason), style = type.bodyText, color = palette.ink)
                        }
                    }
                }
            }

            Section(Res.string.widget_guide_home_title) {
                Steps(HomeSteps)
                if (onAdd != null) {
                    Spacer(Modifier.height(4.dp))
                    BrandButton(stringResource(Res.string.widget_guide_add), onClick = onAdd, modifier = Modifier.fillMaxWidth())
                }
            }

            Section(Res.string.widget_guide_lock_title) {
                Text(stringResource(Res.string.widget_guide_lock_intro), style = type.bodyText, color = palette.inkSoft)
                Steps(LockSteps)
                Text(stringResource(Res.string.widget_guide_privacy), style = type.bodySmall, color = palette.inkMute)
            }
        }
    }
}

@Composable
private fun Section(
    title: StringResource,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(title),
            style = TempusTheme.type.title,
            color = TempusTheme.colors.ink,
            modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() },
        )
        content()
    }
}

/** Numbered steps in a frosted card. */
@Composable
private fun Steps(steps: List<StringResource>) {
    val palette = TempusTheme.colors
    GroupCard {
        steps.forEachIndexed { index, step ->
            if (index > 0) GroupDivider()
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).background(palette.accentSoft, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = TempusTheme.type.button, color = palette.accent)
                }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(step), style = TempusTheme.type.bodyText, color = palette.ink)
            }
        }
    }
}
