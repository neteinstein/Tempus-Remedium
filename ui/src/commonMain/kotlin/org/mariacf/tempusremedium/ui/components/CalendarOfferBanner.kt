package org.mariacf.tempusremedium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.schedule.CalendarOffer
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.calendar_confirm
import org.mariacf.tempusremedium.ui.resources.calendar_confirm_action
import org.mariacf.tempusremedium.ui.resources.calendar_confirm_close
import org.mariacf.tempusremedium.ui.resources.calendar_offer
import org.mariacf.tempusremedium.ui.resources.calendar_offer_action
import org.mariacf.tempusremedium.ui.resources.calendar_offer_close
import org.mariacf.tempusremedium.ui.resources.settings_calendar_stale
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** The web's bar at the top of the app on an iPhone or iPad: add the reminders to the calendar. */
@Composable
fun CalendarOfferBanner(
    offer: CalendarOffer,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CalendarBar(
        text =
            stringResource(
                when (offer) {
                    CalendarOffer.FIRST -> Res.string.calendar_offer
                    CalendarOffer.STALE -> Res.string.settings_calendar_stale
                },
            ),
        action = stringResource(Res.string.calendar_offer_action),
        closeDescription = stringResource(Res.string.calendar_offer_close),
        onAction = onAdd,
        onClose = onClose,
        modifier = modifier,
    )
}

/** After handing over the calendar file: whether the reminders really reached the calendar. */
@Composable
fun CalendarConfirmBanner(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CalendarBar(
        text = stringResource(Res.string.calendar_confirm),
        action = stringResource(Res.string.calendar_confirm_action),
        closeDescription = stringResource(Res.string.calendar_confirm_close),
        onAction = onConfirm,
        onClose = onCancel,
        modifier = modifier,
    )
}

@Composable
private fun CalendarBar(
    text: String,
    action: String,
    closeDescription: String,
    onAction: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(TempusTheme.colors.brandBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .clickable(role = Role.Button, onClick = onAction)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = TempusTheme.type.button,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            action,
            style = TempusTheme.type.button,
            color = Color.White,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = closeDescription, tint = Color.White)
        }
    }
}
