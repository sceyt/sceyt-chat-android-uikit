package com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic

import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash

class PersistenceAttachmentLogicImpl(
        private val messageDao: MessageDao,
        private val messagesCash: MessagesCash) : PersistenceAttachmentLogic {

    override suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadEntity> {
        return messageDao.getAllPayLoadsByMsgTid(tid)
    }

    override fun updateTransferDataByMsgTid(data: TransferData) {
        messageDao.updateAttachmentTransferDataByMsgTid(data.messageTid, data.progressPercent, data.state)
        messagesCash.updateAttachmentTransferData(data)
    }

    override fun updateAttachmentWithTransferData(data: TransferData) {
        messageDao.updateAttachmentAndPayLoad(data)
        messagesCash.updateAttachmentTransferData(data)
    }

    override fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?) {
        messageDao.updateAttachmentFilePathAndMetadata(messageTid, newPath, fileSize, metadata)
        messagesCash.updateAttachmentFilePathAndMeta(messageTid, newPath, metadata)
    }
}