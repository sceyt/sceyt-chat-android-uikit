package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.Job
import java.util.ArrayDeque

/** Serializes short state changes, including reentrant callbacks, without holding a lock over them. */
internal class FileTransferControl {
    private val pending = ArrayDeque<() -> Unit>()
    private val starts = ArrayDeque<Job>()
    private var draining = false

    fun execute(action: () -> Unit) {
        synchronized(pending) {
            pending.addLast(action)
            if (draining) return
            draining = true
        }

        while (true) {
            val next = synchronized(pending) {
                pending.pollFirst() ?: run {
                    draining = false
                    null
                }
            } ?: break
            runCatching(next).onFailure {
                SceytLog.e("FileTransferControl", "File transfer callback failed", it)
            }
        }

        while (true) {
            val job = synchronized(pending) {
                if (draining) return
                starts.pollFirst()
            } ?: return
            job.start()
        }
    }

    /** Called during a state transition; never runs preparation inside the control drain. */
    fun start(job: Job) {
        synchronized(pending) { starts.addLast(job) }
    }
}
