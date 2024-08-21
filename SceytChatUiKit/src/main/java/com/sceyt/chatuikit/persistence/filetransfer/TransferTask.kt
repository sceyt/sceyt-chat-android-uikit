package com.sceyt.chatuikit.persistence.filetransfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

class TransferTask(
        var attachment: SceytAttachment,
        val messageTid: Long,
        var state: TransferState?,
) {
    var progressCallback: ProgressUpdateCallback? = null
    var preparingCallback: PreparingCallback? = null
    var resumePauseCallback: ResumePauseCallback? = null
    var resultCallback: TransferResultCallback? = null
    var updateFileLocationCallback: UpdateFileLocationCallback? = null
    var thumbCallback: ThumbCallback? = null

    val onCompletionListeners: HashMap<String, (success: Boolean, url: String?) -> Unit> by lazy { hashMapOf() }

    fun addOnCompletionListener(key: String, listener: (success: Boolean, url: String?) -> Unit) {
        onCompletionListeners[key] = listener
    }
}