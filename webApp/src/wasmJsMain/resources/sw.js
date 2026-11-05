// Service worker of the installable web app: it keeps every file of the build so the app opens
// without a network, and opens the page a notification points at when it is tapped.
//
// Each build has its own cache, named after VERSION (the Gradle build writes it from the files'
// contents; 'dev' on the development server). The app's files are served from that cache first,
// so a home screen launch is instant and works offline; the page itself (navigations) goes to the
// network first so a new build is noticed. A new build changes this file, so the browser installs
// it, which fills the new cache and removes the old ones.
// precache.json lists the build's files (written by webApp's Gradle build); the development
// server has none, and the app then caches only what it loads.

const VERSION = 'dev';
const PREFIX = 'tempus-remedium';
const CACHE = `${PREFIX}-${VERSION}`;

self.addEventListener('install', (event) => {
    event.waitUntil((async () => {
        const cache = await caches.open(CACHE);
        await cache.addAll(['./', 'index.html', 'manifest.webmanifest']);
        try {
            const response = await fetch('precache.json', { cache: 'no-store' });
            if (response.ok) await cache.addAll(await response.json());
        } catch (e) {
            // No list (development server): runtime caching below covers what the app loads.
        }
        await self.skipWaiting();
    })());
});

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        // The unversioned cache of earlier builds starts with the prefix too.
        await Promise.all(names.filter((name) => name.startsWith(PREFIX) && name !== CACHE).map((name) => caches.delete(name)));
        await self.clients.claim();
    })());
});

const networkFirst = async (request) => {
    const cache = await caches.open(CACHE);
    try {
        const response = await fetch(request);
        if (response.ok) await cache.put(request, response.clone());
        return response;
    } catch (e) {
        const cached = await cache.match(request, { ignoreSearch: request.mode === 'navigate' });
        if (cached) return cached;
        if (request.mode === 'navigate') {
            const shell = await cache.match('./');
            if (shell) return shell;
        }
        throw e;
    }
};

const cacheFirst = async (request) => {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(request);
    if (cached) return cached;
    const response = await fetch(request);
    if (response.ok) await cache.put(request, response.clone());
    return response;
};

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
    const fresh = request.mode === 'navigate' || VERSION === 'dev';
    event.respondWith(fresh ? networkFirst(request) : cacheFirst(request));
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const target = new URL((event.notification.data && event.notification.data.url) || './', self.registration.scope).href;
    event.waitUntil((async () => {
        const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
        if (windows.length > 0) {
            let client = windows[0];
            // Reloads at the target, which opens the slot (see launchRequestFromAddress).
            if (target !== self.registration.scope && 'navigate' in client) {
                client = (await client.navigate(target).catch(() => null)) || client;
            }
            return client.focus();
        }
        return self.clients.openWindow(target);
    })());
});
