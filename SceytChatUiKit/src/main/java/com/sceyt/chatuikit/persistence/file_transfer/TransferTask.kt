package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

class TransferTask(
        attachment: SceytAttachment,
        val messageTid: Long,
        var state: TransferState?,
) {
    @Volatile
    private var _attachment: SceytAttachment = attachment
    
    @PublishedApi
    internal val lock = Any()

    var attachment: SceytAttachment
        get() = _attachment
        set(value) {
            synchronized(lock) {
                _attachment = value
            }
        }

    @PublishedApi
    internal fun getAttachmentInternal(): SceytAttachment = _attachment

    @PublishedApi
    internal fun setAttachmentInternal(value: SceytAttachment) {
        _attachment = value
    }

    /**
     * Atomically updates the attachment if the validation passes.
     * @param validate A function that receives the current attachment and returns true if the update should proceed
     * @param update A function that receives the current attachment and returns the new attachment
     * @return The updated attachment if validation passed, null otherwise
     */
    inline fun updateAttachmentAndStateIfValid(
        validate: (SceytAttachment) -> Boolean,
        update: (SceytAttachment) -> SceytAttachment
    ): SceytAttachment? {
        synchronized(lock) {
            val current = getAttachmentInternal()
            if (!validate(current)) {
                return null
            }
            val updated = update(current)
            setAttachmentInternal(updated)
            state = updated.transferState
            return updated
        }
    }

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