package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask

interface FileTransferLogic {
    fun uploadFile(attachment: SceytAttachment, task: TransferTask)
    fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask)
    fun downloadFile(attachment: SceytAttachment, task: TransferTask)
    fun pauseLoad(attachment: SceytAttachment, state: TransferState)
    fun resumeLoad(attachment: SceytAttachment, state: TransferState)
    fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, data: ThumbData)
    fun clearPreparingThumbPaths()
}