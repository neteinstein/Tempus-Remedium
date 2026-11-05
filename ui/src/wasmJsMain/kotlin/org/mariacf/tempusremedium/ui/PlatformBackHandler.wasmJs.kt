@file:OptIn(ExperimentalWasmJsInterop::class)

package org.mariacf.tempusremedium.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Browser back (and the iPhone's edge swipe in a home screen web app) closes what is open instead
 * of leaving the app: each enabled handler adds a history entry, and going back over it calls the
 * newest handler. A handler that goes away without being used takes its entry back out.
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val id = pushBackEntry { currentOnBack() }
        onDispose { removeBackEntry(id) }
    }
}

@JsFun(
    """(onBack) => {
        let back = window.__tempusBack;
        if (!back) {
            back = window.__tempusBack = { stack: [], ignore: 0, next: 1 };
            window.addEventListener('popstate', () => {
                if (back.ignore > 0) { back.ignore--; return; }
                const top = back.stack.pop();
                if (top) top.onBack();
            });
        }
        const id = back.next++;
        history.pushState({ tempusBack: id }, '');
        back.stack.push({ id: id, onBack: onBack });
        return id;
    }""",
)
private external fun pushBackEntry(onBack: () -> Unit): Int

@JsFun(
    """(id) => {
        const back = window.__tempusBack;
        if (!back) return;
        const index = back.stack.findIndex((entry) => entry.id === id);
        if (index < 0) return;
        back.stack.splice(index, 1);
        back.ignore++;
        history.back();
    }""",
)
private external fun removeBackEntry(id: Int)
