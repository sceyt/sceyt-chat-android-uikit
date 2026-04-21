package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

interface FileTransferService : FileTransferListeners.Listeners {
    fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners)
    fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask
    fun findTransferTask(attachment: SceytAttachment): TransferTask?
    fun addTransferTask(task: TransferTask)
    fun removeTransferTask(messageTid: Long)
    fun getTasks(): Map<String, TransferTask>
    fun clearPreparingThumbPaths()
}