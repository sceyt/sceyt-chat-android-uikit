package com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferTask

interface FileTransferLogic {
    fun uploadFile(attachment: SceytAttachment, task: TransferTask)
    fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask)
    fun downloadFile(attachment: SceytAttachment, task: TransferTask)
    fun pauseLoad(attachment: SceytAttachment, state: TransferState)
    fun resumeLoad(attachment: SceytAttachment, state: TransferState)
    fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData)
    fun clearPreparingThumbPaths()
}