package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.net.Uri
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.toBase64
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.json.JSONObject
import org.koin.core.component.inject

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messageDao by inject<MessageDao>()
    private val messagesCash by inject<MessagesCash>()

    fun download(fileListItem: FileListItem) {
        val message = fileListItem.sceytMessage
        fileTransferService.download(fileListItem.file, TransferTask(message.tid,
            state = fileListItem.file.transferState,
            progressCallback = {
                fileListItem.file.transferState = it.state
                fileListItem.file.progressPercent = it.progressPercent
                MessageEventsObserver.emitAttachmentTransferUpdate(it)
                messageDao.updateAttachmentTransferProgressAndStateWithMsgTid(it.messageTid, it.progressPercent, it.state)
                messagesCash.updateAttachmentTransferData(it)
            }, resultCallback = {
                val attachment = fileListItem.file
                when (it) {
                    is SceytResponse.Success -> {
                        val transferData = TransferData(attachment.messageTid, attachment.tid,
                            100f, TransferState.Downloaded, it.data, attachment.url)
                        attachment.updateWithTransferData(transferData)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                        messageDao.updateAttachmentAndPayLoad(transferData)
                        messagesCash.updateAttachmentTransferData(transferData)
                    }
                    is SceytResponse.Error -> {
                        val transferData = TransferData(
                            attachment.messageTid, attachment.tid, attachment.progressPercent ?: 0f,
                            TransferState.PendingDownload, null, attachment.url)

                        attachment.updateWithTransferData(transferData)
                        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                        messageDao.updateAttachmentAndPayLoad(transferData)
                        messagesCash.updateAttachmentTransferData(transferData)
                    }
                }
            }))
    }

    fun addBlurredBytesAndSizeToMetadata(attachment: SceytAttachment) {
        try {
            attachment.filePath?.let { path ->
                var size: Size? = null
                var base64String: String? = null
                when (attachment.type) {
                    AttachmentTypeEnum.Image.value() -> {
                        size = FileResizeUtil.getImageSize(Uri.parse(path))
                        FileResizeUtil.scaleDownImageByUrl(path, 10f)?.let { bytes ->
                            base64String = bytes.toBase64()
                        }
                    }
                    AttachmentTypeEnum.Video.value() -> {
                        size = FileResizeUtil.getVideoSize(path)
                        FileResizeUtil.scaleDownVideoByUrl(path, 10f)?.let { bytes ->
                            base64String = bytes.toBase64()
                        }
                    }
                }
                setMetadata(base64String, size, attachment)
            }
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't get an blurred image or sizes.")
        }
    }

    fun setMetadata(base64String: String?, size: Size?, attachment: SceytAttachment) {
        try {
            val obj = JSONObject(attachment.metadata.toString())
            obj.put("thumbnail", base64String)
            size?.let {
                obj.put("width", it.width)
                obj.put("height", it.height)
            }
            attachment.metadata = obj.toString()
        } catch (t: Throwable) {
            Log.e(TAG, "Could not parse malformed JSON: \"" + attachment.metadata.toString() + "\"")
        }
    }
}