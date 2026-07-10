package com.sceyt.chatuikit.presentation.common.recyclerview

/**
 * Cancellable token for a single physical scroll request. Returned by the message-list physical
 * scroll methods so a superseding scroll command can [cancel] the previous one: it flips the
 * [cancelled] flag (guarding any posted/deferred blocks) and disposes the bound
 * [ScrollFinishHandle] (removing the pending scroll listener).
 */
class ScrollHandle {
    @Volatile
    var cancelled = false
        private set

    private var finish: ScrollFinishHandle? = null

    fun bindFinish(handle: ScrollFinishHandle) {
        if (cancelled) handle.dispose() else finish = handle
    }

    fun cancel() {
        cancelled = true
        finish?.dispose()
        finish = null
    }
}