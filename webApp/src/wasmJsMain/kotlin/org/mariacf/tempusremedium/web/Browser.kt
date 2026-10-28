@file:OptIn(ExperimentalWasmJsInterop::class)

package org.mariacf.tempusremedium.web

/** iPhone, iPod or iPad (iPadOS reports itself as a Mac, but with touch). */
@JsFun(
    "() => /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)",
)
external fun isIos(): Boolean

@JsFun("() => /Android/i.test(navigator.userAgent)")
external fun isAndroid(): Boolean

/** Opened from the home screen icon rather than in a browser tab. */
@JsFun("() => window.navigator.standalone === true || window.matchMedia('(display-mode: standalone)').matches")
external fun isStandalone(): Boolean

/** iOS only has notifications in a web app opened from the home screen (16.4+). */
@JsFun("() => 'Notification' in window")
external fun notificationsSupported(): Boolean

@JsFun("() => Notification.permission")
external fun notificationPermission(): String

@JsFun("(done) => { Notification.requestPermission().then((result) => done(result)).catch(() => done('denied')); }")
external fun requestNotificationPermission(done: (String) -> Unit)

/** Through the service worker when there is one (Android requires it), else directly. */
@JsFun(
    """(title, body, tag) => {
        const options = { body: body, tag: tag, icon: 'icon-192.png', badge: 'icon-192.png' };
        const direct = () => { try { new Notification(title, options); } catch (e) {} };
        if (!('serviceWorker' in navigator)) { direct(); return; }
        navigator.serviceWorker.getRegistration()
            .then((registration) => registration ? registration.showNotification(title, options) : direct())
            .catch(direct);
    }""",
)
external fun showNotification(
    title: String,
    body: String,
    tag: String,
)

/** The number on the home screen icon; 0 clears it. Silently absent where unsupported. */
@JsFun(
    """(count) => {
        if (!('setAppBadge' in navigator)) return;
        (count > 0 ? navigator.setAppBadge(count) : navigator.clearAppBadge()).catch(() => {});
    }""",
)
external fun setAppBadge(count: Int)

/** Hands [text] to the browser as a file to save or open (on iPhone, a calendar file opens in Calendar). */
@JsFun(
    """(name, type, text) => {
        const url = URL.createObjectURL(new Blob([text], { type: type }));
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    }""",
)
external fun downloadFile(
    name: String,
    type: String,
    text: String,
)

/** Like [downloadFile], for bytes handed over as base64 (a PDF). */
@JsFun(
    """(name, type, base64) => {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const url = URL.createObjectURL(new Blob([bytes], { type: type }));
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    }""",
)
external fun downloadBase64File(
    name: String,
    type: String,
    base64: String,
)

/** Opens the browser's file picker and hands back the picked file's text, or null when cancelled. */
@JsFun(
    """(done) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json,application/json,text/plain';
        input.style.display = 'none';
        let settled = false;
        const finish = (text) => {
            if (settled) return;
            settled = true;
            input.remove();
            done(text);
        };
        input.addEventListener('change', () => {
            const file = input.files && input.files[0];
            if (!file) { finish(null); return; }
            file.text().then(finish).catch(() => finish(null));
        });
        input.addEventListener('cancel', () => finish(null));
        document.body.appendChild(input);
        input.click();
    }""",
)
external fun pickTextFile(done: (String?) -> Unit)
