package org.mariacf.tempusremedium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import org.mariacf.tempusremedium.ui.resources.action_open_permissions
import org.mariacf.tempusremedium.ui.resources.notifications_rationale_body
import org.mariacf.tempusremedium.ui.resources.notifications_rationale_title
import org.mariacf.tempusremedium.ui.resources.permissions_fix
import org.mariacf.tempusremedium.ui.resources.permissions_missing
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** Red bar at the top of the app while a permission reminders need is missing; opens Definições. */
@Composable
fun PermissionsMissingBanner(
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TempusTheme.colors
    // On the danger red, the page background reads as white (light) or near-black (dark).
    val onDanger = palette.background
    Row(
        modifier
            .fillMaxWidth()
            .background(palette.danger)
            .clickable(role = Role.Button, onClick = onFix)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.permissions_missing),
            style = TempusTheme.type.bodySmall,
            color = onDanger,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(Res.string.permissions_fix),
            style = TempusTheme.type.button,
            color = onDanger,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .border(1.dp, onDanger, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Explains why reminders need permissions before the system asks; can also open them in Definições. */
@Composable
fun NotificationsRationaleDialog(
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = TempusTheme.colors.background,
        title = { Text(stringResource(Res.string.notifications_rationale_title), style = TempusTheme.type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.notifications_rationale_body),
                    style = TempusTheme.type.bodyText,
                    color = TempusTheme.colors.inkSoft,
                )
                Text(
                    stringResource(Res.string.action_open_permissions) + " ›",
                    style = TempusTheme.type.button,
                    color = TempusTheme.colors.accent,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onOpenSettings).padding(vertical = 6.dp),
                )
            }
        },
        confirmButton = { BrandButton(stringResource(Res.string.action_allow), onClick = onAllow) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_not_now), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
            }
        },
    )
}
