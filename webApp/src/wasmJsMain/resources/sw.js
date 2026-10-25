// Service worker of the installable web app: it keeps every file of the build so the app opens
// without a network, and brings a notification's page to the front when it is tapped.
//
// Network first, cache as the fallback: online, a new build is always picked up (the HTTP cache
// still avoids re-downloading what did not change); offline, the last files seen are served.
// precache.json lists the build's files (written by webApp's Gradle build); the development
// server has none, and the app then caches only what it loads.

const CACHE = 'tempus-remedium';

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
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
    event.respondWith((async () => {
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
    })());
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    event.waitUntil((async () => {
        const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
        if (windows.length > 0) return windows[0].focus();
        return self.clients.openWindow('./');
    })());
});
