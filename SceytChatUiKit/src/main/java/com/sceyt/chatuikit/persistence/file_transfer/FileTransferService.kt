package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

interface FileTransferService {
    fun upload(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit = {},
    ): TransferTask

    fun uploadSharedFile(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit = {},
    ): TransferTask

    fun download(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit = {},
    ): TransferTask

    fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState)
    /** Supply [channelId] for uploads so channel cleanup also cancels the resumed worker. */
    fun resume(
        messageTid: Long,
        attachment: SceytAttachment,
        state: TransferState,
        channelId: Long? = null,
    )
    /** Cancels this upload without stopping other messages sharing it. */
    fun cancelUpload(attachment: SceytAttachment)

    /** Releases a resumable upload session after its URL and metadata have been persisted. */
    fun uploadPersisted(attachment: SceytAttachment) = Unit

    fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData)
    fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask
    fun findTransferTask(attachment: SceytAttachment): TransferTask?
    /** Removes only this task, leaving other attachments and newer attempts registered. */
    fun removeTransferTask(task: TransferTask)
    fun getTasks(): Map<String, TransferTask>
    fun clearPreparingThumbPaths()
    fun cancelAllTransfers()

    /** Resets a finished attachment whose local file is missing to pending download and downloads it again. */
    fun redownload(attachment: SceytAttachment)
}
