package com.sceyt.chatuikit.persistence.logics.filetransferlogic

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.filetransfer.ThumbData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.filetransfer.TransferTask

interface FileTransferLogic {
    fun uploadFile(attachment: SceytAttachment, task: TransferTask)
    fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask)
    fun downloadFile(attachment: SceytAttachment, task: TransferTask)
    fun pauseLoad(attachment: SceytAttachment, state: TransferState)
    fun resumeLoad(attachment: SceytAttachment, state: TransferState)
    fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData)
    fun clearPreparingThumbPaths()
}