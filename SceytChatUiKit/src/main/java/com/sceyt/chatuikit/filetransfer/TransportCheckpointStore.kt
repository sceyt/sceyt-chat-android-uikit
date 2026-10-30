package com.sceyt.chatuikit.filetransfer

/**
 * Opaque backend state bound to this upload's ciphertext identity.
 * A transport can persist its upload ID, part numbers and ETags here; UIKit stores the string.
 * Save after a part succeeds and await completion before treating that part as durable locally.
 */
interface TransportCheckpointStore {
    val sourceId: String
    suspend fun get(): String?
    suspend fun save(data: String)
}
