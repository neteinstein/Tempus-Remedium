package org.mariacf.tempusremedium.web

import kotlinx.coroutines.suspendCancellableCoroutine
import org.mariacf.tempusremedium.ui.PhotoResult
import org.mariacf.tempusremedium.ui.PhotoStore
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Longest side of a stored photo, in pixels, as on Android. */
private const val PHOTO_MAX_SIDE = 1600

/**
 * Symptom photos in the browser's IndexedDB, which holds far more than localStorage. The browser
 * reduces each photo to an upright JPEG before it is stored. On a phone, its picker also offers
 * the camera, so there is no separate camera button.
 */
object WebPhotoStore : PhotoStore {
    override val canTakePhoto: Boolean = false

    override suspend fun take(): PhotoResult = pick()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun pick(): PhotoResult {
        val base64 =
            suspendCancellableCoroutine<String?> { continuation ->
                pickImageAsJpeg(PHOTO_MAX_SIDE) { continuation.resume(it) }
            } ?: return PhotoResult.Cancelled
        if (base64.isEmpty()) return PhotoResult.Failed
        val id = "${Uuid.random()}.jpg"
        val stored = suspendCancellableCoroutine<Boolean> { continuation -> photoPut(id, base64) { continuation.resume(it) } }
        return if (stored) PhotoResult.Saved(id) else PhotoResult.Failed
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun load(id: String): ByteArray? {
        val base64 = suspendCancellableCoroutine<String?> { continuation -> photoGet(id) { continuation.resume(it) } } ?: return null
        return runCatching { Base64.decode(base64) }.getOrNull()
    }

    override suspend fun delete(id: String) {
        suspendCancellableCoroutine<Unit> { continuation -> photoDelete(id) { continuation.resume(Unit) } }
    }
}

/**
 * Opens the browser's picker for an image and hands back an upright JPEG of it, no larger than
 * [maxSide], as base64; null when cancelled, "" when the file is not an image the browser reads.
 */
@JsFun(
    """(maxSide, done) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.style.display = 'none';
        let settled = false;
        const finish = (value) => {
            if (settled) return;
            settled = true;
            input.remove();
            done(value);
        };
        input.addEventListener('change', () => {
            const file = input.files && input.files[0];
            if (!file) { finish(null); return; }
            createImageBitmap(file, { imageOrientation: 'from-image' }).then((bitmap) => {
                const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
                const canvas = document.createElement('canvas');
                canvas.width = Math.max(1, Math.round(bitmap.width * scale));
                canvas.height = Math.max(1, Math.round(bitmap.height * scale));
                const context = canvas.getContext('2d');
                context.fillStyle = '#ffffff';
                context.fillRect(0, 0, canvas.width, canvas.height);
                context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
                bitmap.close && bitmap.close();
                const url = canvas.toDataURL('image/jpeg', 0.85);
                finish(url.substring(url.indexOf(',') + 1));
            }).catch(() => finish(''));
        });
        input.addEventListener('cancel', () => finish(null));
        document.body.appendChild(input);
        input.click();
    }""",
)
private external fun pickImageAsJpeg(
    maxSide: Int,
    done: (String?) -> Unit,
)

/** Runs [use] with the photo store of the app's IndexedDB database, or calls [failed] when there is none. */
private const val OPEN_DB = """
    const openPhotos = (mode, use, failed) => {
        if (!window.indexedDB) { failed(); return; }
        const request = indexedDB.open('tempus-remedium', 1);
        request.onupgradeneeded = () => request.result.createObjectStore('photos');
        request.onerror = () => failed();
        request.onsuccess = () => {
            const db = request.result;
            const transaction = db.transaction('photos', mode);
            transaction.oncomplete = () => db.close();
            transaction.onabort = () => { db.close(); };
            use(transaction.objectStore('photos'), transaction);
        };
    };
"""

@JsFun(
    """(id, base64, done) => {
        $OPEN_DB
        openPhotos('readwrite', (store, transaction) => {
            store.put(base64, id);
            transaction.oncomplete = () => done(true);
            transaction.onerror = () => done(false);
            transaction.onabort = () => done(false);
        }, () => done(false));
    }""",
)
private external fun photoPut(
    id: String,
    base64: String,
    done: (Boolean) -> Unit,
)

@JsFun(
    """(id, done) => {
        $OPEN_DB
        openPhotos('readonly', (store) => {
            const request = store.get(id);
            request.onsuccess = () => done(typeof request.result === 'string' ? request.result : null);
            request.onerror = () => done(null);
        }, () => done(null));
    }""",
)
private external fun photoGet(
    id: String,
    done: (String?) -> Unit,
)

@JsFun(
    """(id, done) => {
        $OPEN_DB
        openPhotos('readwrite', (store, transaction) => {
            store.delete(id);
            transaction.oncomplete = () => done();
            transaction.onerror = () => done();
            transaction.onabort = () => done();
        }, () => done());
    }""",
)
private external fun photoDelete(
    id: String,
    done: () -> Unit,
)
