package com.sceyt.chatuikit.presentation.common.recyclerview

import androidx.recyclerview.widget.RecyclerView

/**
 * Handle returned by [com.sceyt.chatuikit.extensions.awaitToScrollFinish]. Call [dispose] to
 * remove the pending scroll listener when the awaited scroll is superseded, so listeners don't
 * stack/leak on the [RecyclerView].
 */
class ScrollFinishHandle internal constructor(
    private val recyclerView: RecyclerView,
    private val listener: RecyclerView.OnScrollListener?,
) {
    private var disposed = false

    fun dispose() {
        if (disposed) return
        disposed = true
        listener?.let { recyclerView.removeOnScrollListener(it) }
    }
}