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
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toBase64
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.json.JSONObject
import org.koin.core.component.inject
import java.io.File

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messagesLogic by inject<PersistenceMessagesLogic>()

    fun createTransferTask(attachment: SceytAttachment, isFromUpload: Boolean): TransferTask {
        return TransferTask(messageTid = attachment.messageTid,
            state = attachment.transferState,
            progressCallback = getProgressUpdateCallback(attachment),
            resultCallback = if (isFromUpload) getUploadResultCallback(attachment)
            else getDownloadResultCallback(attachment),
            updateFileLocationCallback = getUpdateFileLocationCallback(attachment))
    }

    fun getProgressUpdateCallback(attachment: SceytAttachment) = ProgressUpdateCallback {
        attachment.transferState = it.state
        attachment.progressPercent = it.progressPercent
        MessageEventsObserver.emitAttachmentTransferUpdate(it)
        messagesLogic.updateTransferDataByMsgTid(it)
    }

    fun getDownloadResultCallback(attachment: SceytAttachment) = TransferResultCallback {
        when (it) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid, attachment.tid,
                    100f, TransferState.Downloaded, it.data, attachment.url)
                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }
            is SceytResponse.Error -> {
                val transferData = TransferData(
                    attachment.messageTid, attachment.tid, attachment.progressPercent ?: 0f,
                    TransferState.ErrorDownload, null, attachment.url)

                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                Log.e(this.TAG, it.message.toString())
            }
        }
    }

    fun getUploadResultCallback(attachment: SceytAttachment) = TransferResultCallback { result ->
        when (result) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.tid, 100f, TransferState.Uploaded, attachment.filePath, result.data.toString())
                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }
            is SceytResponse.Error -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.tid, attachment.progressPercent
                            ?: 0f, TransferState.ErrorUpload, attachment.filePath, null)

                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                Log.e(this.TAG, result.message.toString())
            }
        }
        fileTransferService.findTransferTask(attachment)?.onCompletionListeners?.values?.forEach {
            it.invoke((result is SceytResponse.Success))
        }
    }

    fun getUpdateFileLocationCallback(attachment: SceytAttachment) = UpdateFileLocationCallback { newPath ->
        val transferData = TransferData(attachment.messageTid,
            attachment.tid, 0f, TransferState.FilePathChanged, newPath, attachment.url)

        val newFile = File(newPath)
        if (newFile.exists()) {
            val fileSize = getFileSize(newPath)
            val dimensions = FileResizeUtil.getImageSize(Uri.parse(newPath))
            attachment.filePath = newPath
            attachment.fileSize = fileSize
            attachment.upsertSizeMetadata(dimensions)
            MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
            messagesLogic.updateAttachmentFilePathAndMetadata(attachment.messageTid, newPath, fileSize, attachment.metadata)
        }
    }

    fun SceytAttachment.addBlurredBytesAndSizeToMetadata() {
        try {
            filePath?.let { path ->
                var size: Size? = null
                var base64String: String? = null
                when (type) {
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
                setMetadata(base64String, size)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Couldn't get an blurred image or sizes.")
        }
    }

    private fun SceytAttachment.setMetadata(base64String: String?, size: Size?) {
        try {
            val obj = if (metadata.isNullOrBlank()) JSONObject()
            else JSONObject(metadata.toString())
            obj.put("thumbnail", base64String)
            size?.let {
                obj.put("width", it.width)
                obj.put("height", it.height)
            }
            metadata = obj.toString()
        } catch (t: Throwable) {
            Log.e(TAG, "Could not parse malformed JSON: \"" + metadata.toString() + "\"")
        }
    }

    fun SceytAttachment.upsertSizeMetadata(size: Size?) {
        try {
            val obj = if (metadata.isNullOrBlank()) JSONObject()
            else JSONObject(metadata.toString())
            size?.let {
                obj.put("width", it.width)
                obj.put("height", it.height)
            }
            metadata = obj.toString()
        } catch (t: Throwable) {
            Log.e(TAG, "Could not parse malformed JSON: \"" + metadata.toString() + "\"")
        }
    }
}