package org.mariacf.tempusremedium.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Stores the document in [file], replacing it atomically so a crash mid-write
 * never leaves a truncated document behind. Used by the Android app.
 */
class FileDocumentStorage(
    private val file: File,
) : DocumentStorage {
    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            if (file.exists()) file.readText() else null
        }

    override suspend fun write(content: String) =
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(content)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            Unit
        }

    override suspend fun writeBackup(content: String) =
        withContext(Dispatchers.IO) {
            File(file.parentFile, "${file.name}.corrupt").writeText(content)
        }
}
