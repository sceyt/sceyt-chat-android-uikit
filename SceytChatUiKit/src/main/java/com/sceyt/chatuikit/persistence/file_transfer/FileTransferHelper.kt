package com.sceyt.chatuikit.persistence.file_transfer

import android.util.Size
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getUpsertSizeMetadata
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import kotlin.coroutines.CoroutineContext

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val attachmentLogic by inject<PersistenceAttachmentLogic>()
    private val fileChecksumDao by inject<FileChecksumDao>()
    private val coroutineContext by inject<CoroutineContext>(named(CoroutineContextType.SingleThreaded))
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    private val onTransferUpdatedLiveData_ = MutableLiveData<TransferData>()
    val onTransferUpdatedLiveData: LiveData<TransferData> = onTransferUpdatedLiveData_

    fun createTransferTask(attachment: SceytAttachment): TransferTask {
        return TransferTask(
            attachment = attachment,
            messageTid = attachment.messageTid,
            state = attachment.transferState).apply {
            progressCallback = getProgressUpdateCallback()
            preparingCallback = getPreparingCallback()
            resumePauseCallback = getResumePauseCallback()
            uploadResultCallback = getUploadResultCallback()
            downloadCallback = getDownloadResultCallback()
            updateFileLocationCallback = getUpdateFileLocationCallback()
            thumbCallback = getThumbCallback()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getProgressUpdateCallback() = ProgressUpdateCallback { transferData ->
        attachment = attachment.copy(
            transferState = transferData.state,
            progressPercent = transferData.progressPercent
        )
        attachmentLogic.onTransferProgressPercentUpdated(transferData)
        emitAttachmentTransferUpdate(transferData, attachment.fileSize)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getPreparingCallback() = PreparingCallback { transferData ->
        attachment = attachment.copy(transferState = transferData.state)
        emitAttachmentTransferUpdate(transferData, attachment.fileSize)
        scope.launch {
            attachmentLogic.updateTransferDataByMsgTid(transferData)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getResumePauseCallback() = ResumePauseCallback {
        attachment = attachment.copy(transferState = it.state)
        emitAttachmentTransferUpdate(it, attachment.fileSize)
        scope.launch {
            attachmentLogic.updateTransferDataByMsgTid(it)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getDownloadResultCallback() = TransferResultCallback { response ->
        response.fold(
            onSuccess = { filePath ->
                val transferData = TransferData(attachment.messageTid, 100f,
                    TransferState.Downloaded, filePath, attachment.url)

                attachment = attachment.getUpdatedWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData, attachment.fileSize)
                scope.launch {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                }
            },
            onError = { exception ->
                val transferData = TransferData(
                    attachment.messageTid, attachment.progressPercent ?: 0f,
                    ErrorDownload, null, attachment.url)

                attachment = attachment.getUpdatedWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData, attachment.fileSize)
                scope.launch {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                }
                SceytLog.e(this.TAG, "Couldn't download file url:${attachment.url} error:${exception?.message}")
            }
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getUploadResultCallback() = TransferResultCallback { response ->
        val result: Result<SceytAttachment> = when (response) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid, 100f,
                    Uploaded, attachment.filePath, response.data.toString())

                attachment = attachment.getUpdatedWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData, attachment.fileSize)
                scope.launch {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                }
                Result.success(attachment)
            }

            is SceytResponse.Error -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.progressPercent ?: 0f,
                    ErrorUpload, attachment.filePath, null)

                attachment = attachment.getUpdatedWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData, attachment.fileSize)
                scope.launch {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                }
                SceytLog.e(this.TAG, "Couldn't upload file " + response.message.toString())
                Result.failure(Throwable(response.message.orEmpty()))
            }
        }
        fileTransferService.findTransferTask(attachment)?.onCompletionListeners?.forEach {
            it.value.invoke(result)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getUpdateFileLocationCallback() = UpdateFileLocationCallback { newPath ->
        val transferData = TransferData(attachment.messageTid, 0f,
            TransferState.FilePathChanged, newPath, attachment.url)

        val originalFilePath = attachment.filePath
        val newFile = File(newPath)
        if (newFile.exists()) {
            val fileSize = getFileSize(newPath)
            val dimensions = getDimensions(attachment.type, newPath)
            val metadata = attachment.getUpsertSizeMetadata(dimensions)
            attachment = attachment.copy(filePath = newPath, fileSize = fileSize, metadata = metadata)

            emitAttachmentTransferUpdate(transferData, fileSize)
            scope.launch {
                attachmentLogic.updateAttachmentFilePathAndMetadata(attachment.messageTid, newPath, fileSize, metadata)

                originalFilePath?.let {
                    val checksum = FileChecksumCalculator.calculateFileChecksum(originalFilePath)
                    if (checksum != null)
                        fileChecksumDao.updateResizedFilePathAndSize(checksum, newPath, fileSize)
                }
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getThumbCallback() = ThumbCallback { newPath, thumbData ->
        val transferData = TransferData(attachment.messageTid, attachment.progressPercent ?: 0f,
            TransferState.ThumbLoaded, newPath, attachment.url, thumbData)

        emitAttachmentTransferUpdate(transferData, attachment.fileSize)
    }

    fun emitAttachmentTransferUpdate(transferData: TransferData, fileSize: Long? = null) {
        val data = fileSize?.let {
            val size = getFilePrettySizes(it, transferData.progressPercent)
            transferData.copy(fileLoadedSize = size.first, fileTotalSize = size.second)
        } ?: transferData

        if (transferData.state == TransferState.Downloading
                || transferData.state == TransferState.Uploading)
            onTransferUpdatedLiveData_.postValue(data)
        else {
            scope.launch(Dispatchers.Main.immediate) {
                onTransferUpdatedLiveData_.value = data
            }
        }
    }

    @JvmStatic
    fun getFilePrettySizes(fileSize: Long, progressPercent: Float): Pair<String, String> {
        val format = if (fileSize > 99f) "%.2f" else "%.1f"
        val fileTotalSize = fileSize.toPrettySize()
        val fileLoadedSize = (fileSize * progressPercent / 100).toPrettySize(format)
        return Pair(fileLoadedSize, fileTotalSize)
    }

    private fun getDimensions(type: String, path: String): Size? = try {
        when (type) {
            AttachmentTypeEnum.Image.value -> {
                FileResizeUtil.getImageDimensionsSize(path)
            }

            AttachmentTypeEnum.Video.value -> {
                FileResizeUtil.getVideoSize(path)
            }

            else -> null
        }
    } catch (e: Exception) {
        SceytLog.e(TAG, "Couldn't get dimensions of file $path. Error: ${e.message}")
        null
    }
}