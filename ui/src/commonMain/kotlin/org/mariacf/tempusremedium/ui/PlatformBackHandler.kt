package org.mariacf.tempusremedium.ui

import androidx.compose.runtime.Composable

/** System back (Android back gesture); a no-op on the web. */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
