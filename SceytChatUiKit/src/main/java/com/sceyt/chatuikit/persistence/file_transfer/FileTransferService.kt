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
    fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState)
    fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData)
    fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask
    fun findTransferTask(attachment: SceytAttachment): TransferTask?
    fun removeTransferTask(messageTid: Long)
    fun getTasks(): Map<String, TransferTask>
    fun clearPreparingThumbPaths()
    fun cancelAllTransfers()
}
