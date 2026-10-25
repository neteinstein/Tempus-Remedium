package org.mariacf.tempusremedium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_allow
import org.mariacf.tempusremedium.ui.resources.action_not_now
import org.mariacf.tempusremedium.ui.resources.notifications_blocked
import org.mariacf.tempusremedium.ui.resources.notifications_rationale_body
import org.mariacf.tempusremedium.ui.resources.notifications_rationale_title
import org.mariacf.tempusremedium.ui.resources.notifications_turn_on
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** Red bar at the top of the app while notifications are off, with a link to turn them on. */
@Composable
fun NotificationsBlockedBanner(
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TempusTheme.colors
    // On the danger red, the page background reads as white (light) or near-black (dark).
    val onDanger = palette.background
    Row(
        modifier
            .fillMaxWidth()
            .background(palette.danger)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.notifications_blocked),
            style = TempusTheme.type.bodySmall,
            color = onDanger,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(Res.string.notifications_turn_on),
            style = TempusTheme.type.button,
            color = onDanger,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .border(1.dp, onDanger, CircleShape)
                    .clickable(role = Role.Button, onClick = onTurnOn)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Explains why reminders need notifications before the system asks. */
@Composable
fun NotificationsRationaleDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = TempusTheme.colors.background,
        title = { Text(stringResource(Res.string.notifications_rationale_title), style = TempusTheme.type.title) },
        text = {
            Text(
                stringResource(Res.string.notifications_rationale_body),
                style = TempusTheme.type.bodyText,
                color = TempusTheme.colors.inkSoft,
            )
        },
        confirmButton = { BrandButton(stringResource(Res.string.action_allow), onClick = onAllow) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_not_now), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
            }
        },
    )
}
