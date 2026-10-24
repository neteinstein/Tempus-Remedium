package org.mariacf.tempusremedium.core.data

import kotlinx.browser.localStorage

/** Keeps the document in the browser's localStorage; data never leaves this browser. */
class LocalStorageDocumentStorage(
    private val key: String = "tempus-remedium",
    private val legacyKey: String? = "remedio-em-dia",
) : DocumentStorage {
    // Data saved before the app was renamed is still read until the first write under the new key.
    override suspend fun read(): String? = localStorage.getItem(key) ?: legacyKey?.let { localStorage.getItem(it) }

    override suspend fun write(content: String) {
        localStorage.setItem(key, content)
    }

    override suspend fun writeBackup(content: String) {
        localStorage.setItem("$key.corrupt", content)
    }
}
