package com.sceyt.chatuikit.filetransfer

/** One caller's participation in an operation, independent of the operation's other callers. */
internal interface FileTransferSubscription {
    val isPaused: Boolean
    val isFinished: Boolean

    fun pause()
    fun resume()
    fun cancel()
}

/** File-only notifications translated into attachment state by the attachment adapters. */
internal interface FileTransferObserver {
    fun onStarted() {}
    fun onProgress(progressPercent: Float) {}
    fun onPaused() {}
    fun onResumed(restarted: Boolean) {}
    fun onResult(result: Result<String>)
}
