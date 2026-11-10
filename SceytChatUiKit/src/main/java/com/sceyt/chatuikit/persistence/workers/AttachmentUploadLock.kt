package com.sceyt.chatuikit.persistence.workers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Gives ordinary upload workers one turn for preparation and transfer. */
internal class AttachmentUploadLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
