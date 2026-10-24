package io.github.neteinstein.remedioemdia.core.data

/** Where the single JSON document lives: a file on Android, localStorage on the web. */
interface DocumentStorage {
    suspend fun read(): String?

    suspend fun write(content: String)

    /** Keeps a copy of a document that could not be parsed, so it is never silently lost. */
    suspend fun writeBackup(content: String)
}

class InMemoryDocumentStorage(
    var content: String? = null,
) : DocumentStorage {
    var backup: String? = null
        private set

    override suspend fun read(): String? = content

    override suspend fun write(content: String) {
        this.content = content
    }

    override suspend fun writeBackup(content: String) {
        backup = content
    }
}
