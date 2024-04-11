package com.sceyt.chatuikit.persistence.filetransfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import java.util.concurrent.ConcurrentHashMap

interface FileTransferService : FileTransferListeners.Listeners {
    fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners)
    fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask
    fun findTransferTask(attachment: SceytAttachment): TransferTask?
    fun getTasks(): ConcurrentHashMap<String, TransferTask>
    fun clearPreparingThumbPaths()
}