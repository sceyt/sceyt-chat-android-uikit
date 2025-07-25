package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

class TransferTask(
        var attachment: SceytAttachment,
        val messageTid: Long,
        var state: TransferState?,
) {
    var progressCallback: ProgressUpdateCallback? = null
    var preparingCallback: PreparingCallback? = null
    var resumePauseCallback: ResumePauseCallback? = null
    var uploadResultCallback: TransferResultCallback? = null
    var downloadCallback: TransferResultCallback? = null
    var updateFileLocationCallback: UpdateFileLocationCallback? = null
    var thumbCallback: ThumbCallback? = null

    val onCompletionListeners: HashMap<String, (Result<SceytAttachment>) -> Unit> by lazy { hashMapOf() }

    fun addOnCompletionListener(key: String, listener: (Result<SceytAttachment>) -> Unit) {
        onCompletionListeners[key] = listener
    }
}