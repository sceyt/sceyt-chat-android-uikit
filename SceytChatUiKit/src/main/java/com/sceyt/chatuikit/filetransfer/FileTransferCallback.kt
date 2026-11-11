package com.sceyt.chatuikit.filetransfer

/** Reports transfer progress as a percentage from `0f` to `100f`. */
fun interface FileTransferCallback {
    fun onProgress(progressPercent: Float)
}
