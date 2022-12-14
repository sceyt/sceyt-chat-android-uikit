package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.util.Log
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messageDao by inject<MessageDao>()

    fun download(scope: CoroutineScope, fileListItem: FileListItem) {
        val transferUpdateHelper = DebounceHelper(200L, scope)
        val message = fileListItem.sceytMessage
        fileTransferService.download(fileListItem.file, TransferTask(
            message.tid,
            state = fileListItem.file.transferState,
            progressCallback = {
                fileListItem.file.transferState = it.state
                fileListItem.file.progressPercent = it.progressPercent
                MessageEventsObserver.emitAttachmentTransferUpdate(it)
                scope.launch(Dispatchers.IO) {
                    transferUpdateHelper.submitSuspendable {
                        //messageDao.updateAttachmentTransferDataWithUrl(it.url, it.progressPercent, it.state)
                        messageDao.updateAttachmentTransferProgressAndStateWithMsgTid(it.messageTid, it.progressPercent, it.state)
                    }
                }
            }, resultCallback = {
                transferUpdateHelper.cancelLastDebounce()
                val attachment = fileListItem.file
                when (it) {
                    is SceytResponse.Success -> {
                        attachment.filePath = it.data
                        attachment.progressPercent = 100f
                        attachment.transferState = TransferState.Downloaded
                        val transferData = TransferData(
                            attachment.messageTid, attachment.tid, 100f, TransferState.Downloaded, it.data, attachment.url)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                        scope.launch(Dispatchers.IO) {
                            Log.i("fgfdg",transferData.toString())
                            messageDao.updateAttachmentAndPayLoad(transferData)
                        }
                    }
                    is SceytResponse.Error -> {
                        val lastPercent = attachment.progressPercent ?: 0f
                        attachment.filePath = it.data
                        attachment.progressPercent = 100f
                        attachment.transferState = TransferState.Downloaded

                        val transferData = TransferData(
                            attachment.messageTid, attachment.tid, lastPercent, TransferState.ErrorDownload, null, attachment.url)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)

                        scope.launch(Dispatchers.IO) {
                            messageDao.updateAttachmentAndPayLoad(transferData)
                        }
                    }
                }
            }))
    }

/*    fun upload(scope: CoroutineScope, messageTid: Long, attachment: Attachment) {
        val debounceHelper = DebounceHelper(200L, scope)
        fileTransferService.upload(tmpMessage.tid, it.toAttachment(),
            progressCallback = { updateDate ->
                if (it.fileTransferData?.state != TransferState.Uploaded) {
                    MessageEventsObserver.emitAttachmentTransferUpdate(updateDate)
                    it.fileTransferData = updateDate
                    debounceHelper.submitSuspendable {
                        messageDao.updateAttachmentTransferDataWithTid(updateDate.attachmentTid, updateDate.progressPercent, updateDate.state)
                    }
                }
            },
            resultCallback = { result ->
                debounceHelper.cancelLastDebounce()
                if (result is SceytResponse.Success) {
                    val transferData = TransferData(tmpMessage.tid,
                        it.tid, 100f, TransferState.Uploaded, it.filePath, result.data.toString())
                    it.fileTransferData = transferData
                    it.url = transferData.url
                    MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                    successUploadedAttachments.add(it.tid)
                    messageDao.updateAttachmentTransferData(it.tid, 100f, TransferState.Uploaded, it.filePath, result.data)
                } else {
                    val percent = it.fileTransferData?.progressPercent ?: 0f
                    MessageEventsObserver.emitAttachmentTransferUpdate(TransferData(tmpMessage.tid,
                        it.tid, percent, TransferState.ErrorUpload, it.filePath, null))
                    messageDao.updateAttachmentTransferData(it.tid, percent,
                        TransferState.ErrorUpload, it.filePath, null)
                }
                processedCount++
                if (processedCount == size)
                    continuation.resume(successUploadedAttachments.size == size)
            })
    }

    fun resumeOrPause(scope: CoroutineScope, item: FileListItem) {
        when (val state = item.file.transferState) {
            TransferState.Uploading, TransferState.Downloading -> {
                fileTransferService.pause(item.sceytMessage.tid, item.file.toAttachment(), state)
            }
            TransferState.PendingDownload -> {
                fileTransferService.resume(item.sceytMessage.tid, item.file.toAttachment(), state, { updateDate ->
                    if (it.fileTransferData?.state != TransferState.Uploaded) {
                        MessageEventsObserver.emitAttachmentTransferUpdate(updateDate)
                        it.fileTransferData = updateDate
                        debounceHelper.submitSuspendable {
                            messageDao.updateAttachmentTransferDataWithTid(updateDate.attachmentTid, updateDate.progressPercent, updateDate.state)
                        }
                    }
                }, {

                })
            }
            TransferState.PendingUpload -> {
                upload(scope, item.file, item.sceytMessage.tid, item.file.toAttachment())
            }
            TransferState.ErrorDownload -> download(scope, item)
            TransferState.ErrorUpload -> TODO()
            else -> return
        }
    }*/
}