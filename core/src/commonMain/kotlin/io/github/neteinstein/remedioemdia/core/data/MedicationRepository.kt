package io.github.neteinstein.remedioemdia.core.data

import io.github.neteinstein.remedioemdia.core.model.AppData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Single source of truth for [AppData]. Every change goes through [update], which
 * applies a pure transformation (see `schedule/Mutations.kt`) and persists the result.
 */
class MedicationRepository(
    private val storage: DocumentStorage,
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow(AppData())
    private var loaded = false

    val data: StateFlow<AppData> = state.asStateFlow()

    /** Reads the stored document once; later calls are no-ops. */
    suspend fun load() {
        mutex.withLock {
            if (loaded) return
            state.value = decodeOrBackup(storage.read())
            loaded = true
        }
    }

    suspend fun update(transform: (AppData) -> AppData) {
        load()
        mutex.withLock {
            val next = transform(state.value)
            if (next == state.value) return
            storage.write(json.encodeToString(AppData.serializer(), next))
            state.value = next
        }
    }

    private suspend fun decodeOrBackup(raw: String?): AppData {
        if (raw.isNullOrBlank()) return AppData()
        return try {
            json.decodeFromString(AppData.serializer(), raw)
        } catch (e: SerializationException) {
            storage.writeBackup(raw)
            AppData()
        } catch (e: IllegalArgumentException) {
            storage.writeBackup(raw)
            AppData()
        }
    }

    companion object {
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}
