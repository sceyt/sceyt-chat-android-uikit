package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import org.koin.core.component.inject

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messageDao by inject<MessageDao>()

    fun download(fileListItem: FileListItem) {
        val message = fileListItem.sceytMessage
        fileTransferService.download(fileListItem.file, TransferTask(message.tid,
            state = fileListItem.file.transferState,
            progressCallback = {
                fileListItem.file.transferState = it.state
                fileListItem.file.progressPercent = it.progressPercent
                MessageEventsObserver.emitAttachmentTransferUpdate(it)
                messageDao.updateAttachmentTransferProgressAndStateWithMsgTid(it.messageTid, it.progressPercent, it.state)
            }, resultCallback = {
                val attachment = fileListItem.file
                when (it) {
                    is SceytResponse.Success -> {
                        val transferData = TransferData(attachment.messageTid, attachment.tid,
                            100f, TransferState.Downloaded, it.data, attachment.url)
                        attachment.updateWithTransferData(transferData)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                        messageDao.updateAttachmentAndPayLoad(transferData)
                    }
                    is SceytResponse.Error -> {
                        val transferData = TransferData(
                            attachment.messageTid, attachment.tid, attachment.progressPercent ?: 0f,
                            TransferState.PendingDownload, null, attachment.url)

                        attachment.updateWithTransferData(transferData)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                        messageDao.updateAttachmentAndPayLoad(transferData)
                    }
                }
            }))
    }
}