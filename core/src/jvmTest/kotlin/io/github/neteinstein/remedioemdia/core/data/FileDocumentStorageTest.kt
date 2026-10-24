package io.github.neteinstein.remedioemdia.core.data

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FileDocumentStorageTest {
    private val dir: File = Files.createTempDirectory("storage").toFile()
    private val file = File(dir, "nested/data.json")

    @Test
    fun readsNothingBeforeTheFirstWrite() =
        runTest {
            assertNull(FileDocumentStorage(file).read())
        }

    @Test
    fun writeReplacesTheDocument() =
        runTest {
            val storage = FileDocumentStorage(file)
            storage.write("one")
            storage.write("two")
            assertEquals("two", storage.read())
            assertFalse(File(file.parentFile, "data.json.tmp").exists())
        }

    @Test
    fun backupGoesNextToTheDocument() =
        runTest {
            val storage = FileDocumentStorage(file)
            storage.write("current")
            storage.writeBackup("broken")
            assertEquals("broken", File(file.parentFile, "data.json.corrupt").readText())
            assertEquals("current", storage.read())
        }
}
