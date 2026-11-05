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

/**
 * Through the service worker when there is one (Android requires it), else directly. Tapping it
 * opens [url] (relative to the app, e.g. "./?slot=MORNING"); see sw.js.
 */
@JsFun(
    """(title, body, tag, url) => {
        const options = { body: body, tag: tag, icon: 'icon-192.png', badge: 'icon-192.png', data: { url: url } };
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
    url: String,
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

/**
 * On iPhone, the share sheet (Save to Files, AirDrop, Mail…): a download in a home screen web app
 * has nowhere to go. Falls back to [downloadBase64File] where files cannot be shared, or when the
 * browser refuses (sharing needs a recent tap, which a slow PDF may outlast).
 */
@JsFun(
    """(name, type, base64) => {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const download = () => {
            const url = URL.createObjectURL(new Blob([bytes], { type: type }));
            const link = document.createElement('a');
            link.href = url;
            link.download = name;
            document.body.appendChild(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 60000);
        };
        let file;
        try { file = new File([bytes], name, { type: type }); } catch (e) { download(); return; }
        if (!navigator.canShare || !navigator.canShare({ files: [file] })) { download(); return; }
        navigator.share({ files: [file], title: name }).catch((error) => {
            if (!error || error.name !== 'AbortError') download();
        });
    }""",
)
external fun shareBase64File(
    name: String,
    type: String,
    base64: String,
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

/** Calls [onVisible] each time the page comes back to the front (iOS suspends it in the background). */
@JsFun("(onVisible) => document.addEventListener('visibilitychange', () => { if (!document.hidden) onVisible(); })")
external fun onPageVisible(onVisible: () -> Unit)

/** The browser's top bar and the status bar of a home screen web app, following the in-app theme. */
@JsFun(
    """(color) => {
        document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => meta.remove());
        const meta = document.createElement('meta');
        meta.name = 'theme-color';
        meta.content = color;
        document.head.appendChild(meta);
    }""",
)
external fun setThemeColor(color: String)

/** Asks the browser not to clear the app's storage when space runs low or the site goes unused. */
@JsFun("() => { if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {}); }")
external fun persistStorage()

/** A parameter of the page's address ("slot" in ./?slot=MORNING), or null. */
@JsFun("(name) => new URLSearchParams(window.location.search).get(name)")
external fun queryParameter(name: String): String?

/** Drops the parameters from the address, so a reload does not open the same thing again. */
@JsFun("() => { if (window.location.search) history.replaceState(history.state, '', window.location.pathname); }")
external fun clearQuery()
