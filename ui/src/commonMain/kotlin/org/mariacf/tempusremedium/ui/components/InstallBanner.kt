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
import org.mariacf.tempusremedium.ui.InstallPrompt
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.install_banner_close
import org.mariacf.tempusremedium.ui.resources.install_home_screen
import org.mariacf.tempusremedium.ui.resources.install_store
import org.mariacf.tempusremedium.ui.resources.install_store_action
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * The web's bar at the top of the app: on an iPhone or iPad, how to add it to the home screen;
 * on Android, the native app on the store.
 */
@Composable
fun InstallBanner(
    prompt: InstallPrompt,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = prompt as? InstallPrompt.Store
    Row(
        modifier
            .fillMaxWidth()
            .background(TempusTheme.colors.brandBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(if (store != null) Modifier.clickable(role = Role.Button, onClick = store.open) else Modifier)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (store != null) Res.string.install_store else Res.string.install_home_screen),
            style = TempusTheme.type.button,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        if (store != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(Res.string.install_store_action),
                style = TempusTheme.type.button,
                color = Color.White,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.install_banner_close), tint = Color.White)
        }
    }
}
