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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val attachmentLogic by inject<PersistenceAttachmentLogic>()
    private val fileChecksumDao by inject<FileChecksumDao>()
    private val coroutineContext by inject<CoroutineContext>(named(CoroutineContextType.SingleThreaded))
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    private val onTransferUpdatedLiveData_ = MutableLiveData<TransferData>()
    val onTransferUpdatedLiveData: LiveData<TransferData> = onTransferUpdatedLiveData_

    // Per-attachment mutex for sequential updates
    private val attachmentMutexes = ConcurrentHashMap<Long, Mutex>()

    /**
     * Gets or creates a mutex for the given messageTid to ensure sequential updates per attachment.
     */
    private fun getMutexForAttachment(messageTid: Long): Mutex {
        return attachmentMutexes.getOrPut(messageTid) { Mutex() }
    }

    /**
     * Removes the mutex when transfer is complete to prevent memory leaks.
     */
    private fun removeMutexForAttachment(messageTid: Long) {
        attachmentMutexes.remove(messageTid)
    }

    /**
     * Enqueues a database update to be processed sequentially for a specific attachment.
     */
    private fun enqueueDbUpdate(messageTid: Long, update: suspend () -> Unit) {
        scope.launch {
            getMutexForAttachment(messageTid).withLock {
                update()
            }
        }
    }

    fun createTransferTask(attachment: SceytAttachment): TransferTask {
        return TransferTask(
            attachment = attachment,
            messageTid = attachment.messageTid,
            state = attachment.transferState
        ).apply {
            progressCallback = getProgressUpdateCallback()
            preparingCallback = getPreparingCallback()
            resumePauseCallback = getResumePauseCallback()
            uploadResultCallback = getUploadResultCallback()
            downloadCallback = getDownloadResultCallback()
            updateFileLocationCallback = getUpdateFileLocationCallback()
            thumbCallback = getThumbCallback()
        }
    }

    fun TransferTask.getProgressUpdateCallback() = ProgressUpdateCallback { transferData ->
        val updatedAttachment = updateAttachmentAndStateIfValid(
            validate = { current ->
                TransferStateValidator.isValidStateTransition(
                    currentState = current.transferState,
                    newState = transferData.state,
                    currentProgress = current.progressPercent ?: 0f,
                    newProgress = transferData.progressPercent
                )
            },
            update = { current ->
                current.copy(
                    transferState = transferData.state,
                    progressPercent = transferData.progressPercent
                )
            }
        ) ?: return@ProgressUpdateCallback

        emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
        scope.launch {
            attachmentLogic.onTransferProgressPercentUpdated(transferData)
        }
    }

    fun TransferTask.getPreparingCallback() = PreparingCallback { transferData ->
        val updatedAttachment = updateAttachmentAndStateIfValid(
            validate = { current ->
                TransferStateValidator.isValidStateTransition(
                    currentState = current.transferState,
                    newState = transferData.state,
                    currentProgress = current.progressPercent ?: 0f,
                    newProgress = transferData.progressPercent
                )
            },
            update = { current ->
                current.copy(transferState = transferData.state)
            }
        ) ?: return@PreparingCallback

        emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
        enqueueDbUpdate(messageTid) {
            attachmentLogic.updateTransferDataByMsgTid(transferData)
        }
    }

    fun TransferTask.getResumePauseCallback() = ResumePauseCallback { transferData ->
        val updatedAttachment = updateAttachmentAndStateIfValid(
            validate = { current ->
                TransferStateValidator.isValidStateTransition(
                    currentState = current.transferState,
                    newState = transferData.state,
                    currentProgress = current.progressPercent ?: 0f,
                    newProgress = transferData.progressPercent
                )
            },
            update = { current ->
                current.copy(transferState = transferData.state)
            }
        ) ?: return@ResumePauseCallback

        emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
        enqueueDbUpdate(messageTid) {
            attachmentLogic.updateTransferDataByMsgTid(transferData)
        }
    }

    fun TransferTask.getDownloadResultCallback() = TransferResultCallback { response ->
        response.fold(
            onSuccess = { filePath ->
                val currentAttachment = attachment
                val transferData = TransferData(
                    currentAttachment.messageTid, 100f,
                    TransferState.Downloaded, filePath, currentAttachment.url
                )

                val updatedAttachment = updateAttachmentAndStateIfValid(
                    validate = { true }, // Result callbacks always succeed
                    update = { it.getUpdatedWithTransferData(transferData) }
                ) ?: return@fold

                emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
                fileTransferService.removeTransferTask(messageTid)
                enqueueDbUpdate(messageTid) {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                    removeMutexForAttachment(messageTid)
                }
            },
            onError = { exception ->
                val currentAttachment = attachment
                val transferData = TransferData(
                    currentAttachment.messageTid, currentAttachment.progressPercent ?: 0f,
                    ErrorDownload, null, currentAttachment.url
                )

                val updatedAttachment = updateAttachmentAndStateIfValid(
                    validate = { true }, // Result callbacks always succeed
                    update = { it.getUpdatedWithTransferData(transferData) }
                ) ?: return@fold

                emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
                fileTransferService.removeTransferTask(messageTid)
                enqueueDbUpdate(messageTid) {
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                }
                SceytLog.e(
                    this.TAG,
                    "Couldn't download file url:${currentAttachment.url} error:${exception?.message}"
                )
            }
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getUploadResultCallback() = TransferResultCallback { response ->
        val result: Result<SceytAttachment> = when (response) {
            is SceytResponse.Success -> {
                val currentAttachment = attachment
                val transferData = TransferData(
                    currentAttachment.messageTid, 100f,
                    Uploaded, currentAttachment.filePath, response.data.toString()
                )

                val updatedAttachment = updateAttachmentAndStateIfValid(
                    validate = { true }, // Result callbacks always succeed
                    update = { it.getUpdatedWithTransferData(transferData) }
                )

                if (updatedAttachment != null) {
                    emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
                    enqueueDbUpdate(messageTid) {
                        attachmentLogic.updateAttachmentWithTransferData(transferData)
                        removeMutexForAttachment(messageTid)
                    }
                    Result.success(updatedAttachment)
                } else {
                    Result.failure(Throwable("Failed to update attachment"))
                }
            }

            is SceytResponse.Error -> {
                val currentAttachment = attachment
                val transferData = TransferData(
                    currentAttachment.messageTid,
                    currentAttachment.progressPercent ?: 0f,
                    ErrorUpload, currentAttachment.filePath, null
                )

                val updatedAttachment = updateAttachmentAndStateIfValid(
                    validate = { true }, // Result callbacks always succeed
                    update = { it.getUpdatedWithTransferData(transferData) }
                )

                if (updatedAttachment != null) {
                    emitAttachmentTransferUpdate(transferData, updatedAttachment.fileSize)
                    enqueueDbUpdate(messageTid) {
                        attachmentLogic.updateAttachmentWithTransferData(transferData)
                    }
                }
                SceytLog.e(this.TAG, "Couldn't upload file " + response.message.toString())
                Result.failure(Throwable(response.message.orEmpty()))
            }
        }
        fileTransferService.findTransferTask(attachment)?.onCompletionListeners?.forEach {
            it.value.invoke(result)
        }
        fileTransferService.removeTransferTask(messageTid)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getUpdateFileLocationCallback() = UpdateFileLocationCallback { newPath ->
        val newFile = File(newPath)
        if (!newFile.exists()) return@UpdateFileLocationCallback

        val currentAttachment = attachment
        val transferData = TransferData(
            currentAttachment.messageTid, 0f,
            TransferState.FilePathChanged, newPath, currentAttachment.url
        )

        val originalFilePath = currentAttachment.filePath
        val fileSize = getFileSize(newPath)
        val dimensions = getDimensions(currentAttachment.type, newPath)
        val metadata = currentAttachment.getUpsertSizeMetadata(dimensions)

        val updatedAttachment = updateAttachmentAndStateIfValid(
            validate = { true }, // FilePathChanged is always allowed
            update = { it.copy(filePath = newPath, fileSize = fileSize, metadata = metadata) }
        ) ?: return@UpdateFileLocationCallback

        emitAttachmentTransferUpdate(transferData, fileSize)
        enqueueDbUpdate(messageTid) {
            attachmentLogic.updateAttachmentFilePathAndMetadata(
                updatedAttachment.messageTid,
                newPath,
                fileSize,
                metadata
            )

            originalFilePath?.let {
                val checksum = FileChecksumCalculator.calculateFileChecksum(originalFilePath)
                if (checksum != null)
                    fileChecksumDao.updateResizedFilePathAndSize(checksum, newPath, fileSize)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun TransferTask.getThumbCallback() = ThumbCallback { newPath, thumbData ->
        val transferData = TransferData(
            attachment.messageTid, attachment.progressPercent ?: 0f,
            TransferState.ThumbLoaded, newPath, attachment.url, thumbData
        )

        emitAttachmentTransferUpdate(transferData, attachment.fileSize)
    }

    fun emitAttachmentTransferUpdate(transferData: TransferData, fileSize: Long? = null) {
        val data = fileSize?.let {
            val size = getFilePrettySizes(it, transferData.progressPercent)
            transferData.copy(fileLoadedSize = size.first, fileTotalSize = size.second)
        } ?: transferData
        val acceptedData = AttachmentTransferStateStore.put(data) ?: return

        if (acceptedData.state == TransferState.Downloading || acceptedData.state == TransferState.Uploading) {
            onTransferUpdatedLiveData_.postValue(acceptedData)
        } else {
            scope.launch(Dispatchers.Main.immediate) {
                onTransferUpdatedLiveData_.value = acceptedData
            }
        }
    }

    @JvmStatic
    fun getFilePrettySizes(fileSize: Long, progressPercent: Float): Pair<String, String> {
        return TransferData.getFilePrettySizes(fileSize, progressPercent)
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