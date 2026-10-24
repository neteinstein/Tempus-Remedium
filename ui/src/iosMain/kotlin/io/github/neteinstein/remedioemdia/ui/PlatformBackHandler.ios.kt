package io.github.neteinstein.remedioemdia.ui

import androidx.compose.runtime.Composable

// iOS has no system back button; the editor's own back arrow closes it.
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
