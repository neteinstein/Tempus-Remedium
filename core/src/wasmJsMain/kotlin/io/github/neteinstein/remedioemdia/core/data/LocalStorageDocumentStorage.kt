package io.github.neteinstein.remedioemdia.core.data

import kotlinx.browser.localStorage

/** Keeps the document in the browser's localStorage; data never leaves this browser. */
class LocalStorageDocumentStorage(
    private val key: String = "remedio-em-dia",
) : DocumentStorage {
    override suspend fun read(): String? = localStorage.getItem(key)

    override suspend fun write(content: String) {
        localStorage.setItem(key, content)
    }

    override suspend fun writeBackup(content: String) {
        localStorage.setItem("$key.corrupt", content)
    }
}
