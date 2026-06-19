package com.sceyt.chatuikit.persistence.logicimpl.channel

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PendingChannelMigrationLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        return mutex.withLock { block() }
    }
}
