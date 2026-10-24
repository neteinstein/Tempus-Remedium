package io.github.neteinstein.remedioemdia.android.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/** Glance compositions have no lifecycle owner, so use the plain collector. */
@Composable
internal fun <T> StateFlow<T>.collectAsStateCompat(): State<T> = collectAsState()
