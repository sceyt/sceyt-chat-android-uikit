package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment

data class TransferTask(
        val attachment: SceytAttachment,
        val messageTid: Long,
        var state: TransferState?,
        val progressCallback: ProgressUpdateCallback,
        val preparingCallback: PreparingCallback,
        val resumePauseCallback: ResumePauseCallback,
        val resultCallback: TransferResultCallback,
        val updateFileLocationCallback: UpdateFileLocationCallback,
        val thumbCallback: ThumbCallback) {

    val onCompletionListeners: HashMap<String, (success: Boolean, url: String?) -> Unit> by lazy { hashMapOf() }

    fun addOnCompletionListener(key: String, listener: (success: Boolean, url: String?) -> Unit) {
        onCompletionListeners[key] = listener
    }
}