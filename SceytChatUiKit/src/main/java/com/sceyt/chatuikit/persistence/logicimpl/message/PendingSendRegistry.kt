package com.sceyt.chatuikit.persistence.logicimpl.message

import java.util.concurrent.ConcurrentHashMap

internal class PendingSendRegistry {
    private val inFlight: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun tryAcquire(tid: Long): Boolean = inFlight.add(tid)

    fun release(tid: Long) {
        inFlight.remove(tid)
    }
}
