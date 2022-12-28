package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferTask(
        val messageTid: Long,
        val state: TransferState?,
        val progressCallback: ProgressUpdateCallback,
        val resultCallback: TransferResultCallback,
        val updateFileLocationCallback: UpdateFileLocationCallback,
        val thumbCallback: ThumbCallback) {

    val onCompletionListeners: HashMap<String, (Boolean) -> Unit> by lazy { hashMapOf() }

    fun addOnCompletionListener(key: String, listener: (Boolean) -> Unit) {
        onCompletionListeners[key] = listener
    }
}