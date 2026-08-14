package com.sceyt.chatuikit.filetransfer

interface FileTransferCallback<T> {
    fun onProgress(progressPercent: Float)

    fun onWaitingForNetwork() = Unit

    fun onSuccess(result: T?)

    fun onFailure(error: Throwable?)
}
