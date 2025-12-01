package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.transcodeVideo
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import com.sceyt.chatuikit.shared.utils.FilePathUtil.getOrCreateUniqueFileDirectory
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import kotlin.math.max

internal class FileTransferLogicImpl(
        private val context: Context,
        private val attachmentLogic: PersistenceAttachmentLogic,
) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val transferUtility by lazy { FileTransferUtility() }
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, ThumbPathsData>()
    private var preparingThumbsMap = hashMapOf<Long, Long>()
    private var pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private var currentUploadingAttachment: SceytAttachment? = null
    private var pausedTasksMap = hashMapOf<Long, Long>()
    private var resizingAttachmentsMap = hashMapOf<Long, String>()

    private var sharingFilesPath = Collections.synchronizedSet<ShareFilesData>(mutableSetOf())

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        checkAndUpload(attachment, task)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        fileTransferService.getTasks()[task.messageTid.toString()] = task
        val data = ShareFilesData(attachment.originalFilePath.toString(), attachment.messageTid)
        if (sharingFilesPath.none { it.originalPath == attachment.originalFilePath }) {
            val checksum = getAttachmentChecksum(attachment.originalFilePath)

            val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)
            if (uploaded && url != null) {
                sharingFilesPath.add(data)
                getAppropriateTasks(task).forEach { transferTask ->
                    transferTask.uploadResultCallback?.onResult(SceytResponse.Success(url))
                }
                removeFromSharingPath(attachment.originalFilePath)
                return
            }

            var uploadAttachment = attachment
            checkAndResizeMessageAttachments(context, attachment, checksum, task) {
                if (it.isSuccess) {
                    it.getOrNull()?.let { path ->
                        task.updateFileLocationCallback?.onUpdateFileLocation(path)
                        uploadAttachment = uploadAttachment.copy(
                            filePath = path,
                            fileSize = getFileSize(path)
                        )
                    }
                } else SceytLog.i("resizeResult", "Couldn't resize sharing file with reason ${it.exceptionOrNull()}")

                uploadSharedAttachment(uploadAttachment, task)
            }
        }
        sharingFilesPath.add(data)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        if (attachment.url.isNullOrBlank()) {
            task.downloadCallback?.onResult(
                SceytResponse.Error(SceytException(0, "Wrong url"))
            )
            return
        }
        val destFile = getDestinationFile(context, attachment)
        val file = attachment.isAttachmentExistAndFullyLoaded(destFile)

        if (file != null) {
            task.downloadCallback?.onResult(SceytResponse.Success(file.path))
        } else {
            val downloadMapKey = attachment.downloadMapKey
            if (downloadingUrlMap[downloadMapKey] != null) return
            downloadingUrlMap[downloadMapKey] = downloadMapKey
            pausedTasksMap.remove(attachment.messageTid)

            destFile.deleteOnExit()
            destFile.createNewFile()

            task.progressCallback?.onProgress(TransferData(
                messageTid = task.messageTid,
                progressPercent = attachment.progressPercent ?: 0f,
                state = Downloading,
                filePath = attachment.filePath,
                url = attachment.url)
            )

            transferUtility.downloadFile(
                attachment = attachment,
                destFile = destFile,
                onProgress = { progressPercent ->
                    if (pausedTasksMap[attachment.messageTid] == null) {
                        task.progressCallback?.onProgress(TransferData(
                            task.messageTid, progressPercent, Downloading, null, attachment.url))
                    }
                },
                onResult = {
                    if (it is SceytResponse.Error)
                        destFile.delete()

                    task.downloadCallback?.onResult(it)
                    downloadingUrlMap.remove(downloadMapKey)
                })
        }
    }

    override fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, Uploading, Preparing, FilePathChanged, WaitingToUpload -> {
                pausedTasksMap[attachment.messageTid] = attachment.messageTid
                if (attachment.type == AttachmentTypeEnum.Video.value)
                    VideoTranscodeHelper.cancel(attachment.filePath)

                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = PauseUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
                }

                transferUtility.pauseUpload(attachment)
                uploadNext()
            }

            PendingDownload, Downloading -> {
                pausedTasksMap[attachment.messageTid] = attachment.messageTid
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = PauseUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
                }

                transferUtility.pauseDownload(attachment)
            }

            else -> return
        }
    }

    override fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, PauseUpload, ErrorUpload -> {
                pausedTasksMap.remove(attachment.messageTid)
                var wasSharing = false
                // Try to found sharing attachment with same original path, maybe it was uploaded with another message
                if (sharingFilesPath.any { it.originalPath == attachment.originalFilePath }) {
                    wasSharing = true
                    fileTransferService.getTasks().values.find {
                        it.attachment.filePath == attachment.filePath
                                && it.attachment.messageTid != attachment.messageTid
                                && it.attachment.transferState == Uploaded
                    }?.let {
                        if (it.attachment.url != null) {
                            // If found uploaded attachment with same original path, invoke result callback with url and return
                            fileTransferService.findTransferTask(attachment)?.uploadResultCallback?.onResult(
                                SceytResponse.Success(it.attachment.url)
                            )
                            return
                        }
                    }
                }

                fileTransferService.findTransferTask(attachment)?.also {
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(WaitingToUpload))
                }

                if (!transferUtility.resumeUpload(attachment)) {
                    if (resizingAttachmentsMap[attachment.messageTid] == null) {
                        if (wasSharing)
                            uploadSharedFile(
                                attachment,
                                fileTransferService.findOrCreateTransferTask(attachment)
                            )
                        else
                            uploadFile(
                                attachment,
                                fileTransferService.findOrCreateTransferTask(attachment)
                            )
                    }
                }
            }

            PendingDownload, PauseDownload, ErrorDownload -> {
                pausedTasksMap.remove(attachment.messageTid)
                val destFile = getDestinationFile(context, attachment)
                val file = attachment.isAttachmentExistAndFullyLoaded(destFile)

                if (file != null) {
                    fileTransferService.findTransferTask(attachment)?.downloadCallback?.onResult(
                        SceytResponse.Success(file.path)
                    )
                } else {
                    if (!transferUtility.resumeDownload(attachment)) {
                        downloadingUrlMap.remove(attachment.downloadMapKey)
                        downloadFile(attachment, fileTransferService.findOrCreateTransferTask(attachment))
                    }

                    fileTransferService.findTransferTask(attachment)?.resumePauseCallback?.onResumePause(
                        attachment.toTransferData(Downloading)
                    )
                }
            }

            else -> return
        }
    }

    override fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, data: ThumbData) {
        attachment.filePath ?: return
        val size = data.size
        val thumbKey = getPreparingThumbKey(attachment, data)

        if (preparingThumbsMap[messageTid] != null) return
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val readyThumb = thumbPaths[thumbKey]
        if (readyThumb != null) {
            task.thumbCallback?.onThumb(readyThumb.path, data)
            return
        } else {
            preparingThumbsMap[messageTid] = messageTid
            val result = getAttachmentThumbPath(context, attachment, size)
            if (result.isSuccess) {
                result.getOrNull()?.let { path ->
                    thumbPaths[thumbKey] = ThumbPathsData(messageTid, path, size)
                    task.thumbCallback?.onThumb(path, data)
                }
            } else {
                SceytLog.e(TAG, "Couldn't get a thumb for messageTid: $messageTid," +
                        " path:${attachment.filePath} with reason ${result.exceptionOrNull()}")
            }
        }
        preparingThumbsMap.remove(messageTid)
    }

    override fun clearPreparingThumbPaths() {
        preparingThumbsMap.clear()
    }

    private fun checkAndUpload(attachment: SceytAttachment, task: TransferTask) {
        if (currentUploadingAttachment == null) {
            uploadAttachment(attachment, task)
        } else {
            val alreadyExist = currentUploadingAttachment?.messageTid == attachment.messageTid ||
                    pendingUploadQueue.any { it.first.messageTid == attachment.messageTid }

            if (!alreadyExist)
                pendingUploadQueue.add(Pair(attachment, task))
        }
    }

    private fun uploadNext() {
        currentUploadingAttachment = null
        if (pendingUploadQueue.isEmpty()) return
        pendingUploadQueue.poll()?.let { (attachment, transferTask) ->
            uploadAttachment(attachment, transferTask)
        }
    }

    private fun uploadAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        currentUploadingAttachment = attachment
        val checksum = getAttachmentChecksum(attachment.originalFilePath)

        val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, transferTask)
        if (uploaded && url != null) {
            transferTask.uploadResultCallback?.onResult(SceytResponse.Success(url))
            uploadNext()
            return
        }
        var uploadAttachment = attachment
        checkAndResizeMessageAttachments(context, attachment, checksum, transferTask) {
            // Check if task was paused
            if (pausedTasksMap[attachment.messageTid] != null) {
                uploadNext()
                return@checkAndResizeMessageAttachments
            }

            if (it.isSuccess) {
                it.getOrNull()?.let { path ->
                    transferTask.updateFileLocationCallback?.onUpdateFileLocation(path)
                    uploadAttachment = uploadAttachment.copy(
                        filePath = path,
                        fileSize = getFileSize(path)
                    )
                }
            } else SceytLog.i("resizeResult", "Couldn't resize file with reason ${it.exceptionOrNull()}")

            if (!transferUtility.resumeUpload(attachment)) {
                transferUtility.uploadFile(uploadAttachment,
                    onProgress = { progressPercent ->
                        transferTask.progressCallback?.onProgress(TransferData(transferTask.messageTid,
                            progressPercent, Uploading, uploadAttachment.filePath, null))
                    },
                    onResult = { response ->
                        transferTask.uploadResultCallback?.onResult(response)
                        uploadNext()
                    })
            }
        }
    }

    private fun uploadSharedAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        transferUtility.uploadFile(
            attachment = attachment,
            onProgress = { progressPercent ->
                if (pausedTasksMap[attachment.messageTid] != null) return@uploadFile
                getAppropriateTasks(transferTask).forEach { task ->
                    fileTransferService.getTasks()[task.messageTid.toString()]?.state = Uploading
                    task.progressCallback?.onProgress(TransferData(task.messageTid,
                        progressPercent, Uploading, task.attachment.filePath, null))
                }
            },
            onResult = { response ->
                getAppropriateTasks(transferTask).forEach { task ->
                    task.uploadResultCallback?.onResult(response)
                }
                removeFromSharingPath(attachment.originalFilePath)
            })
    }

    private fun checkAndResizeMessageAttachments(
            context: Context,
            attachment: SceytAttachment,
            checksumData: FileChecksumData?,
            task: TransferTask, callback: (Result<String?>) -> Unit,
    ) {

        val path = checksumData?.resizedFilePath
        if (path != null && File(path).exists()) {
            callback(Result.success(path))
            return
        }
        when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                resizingAttachmentsMap[attachment.messageTid] = attachment.messageTid.toString()
                val reqSize = SceytChatUIKit.config.imageAttachmentResizeConfig.dimensionThreshold
                val quality = SceytChatUIKit.config.imageAttachmentResizeConfig.compressionQuality
                val result = resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = reqSize,
                    quality = quality
                )
                resizingAttachmentsMap.remove(attachment.messageTid)
                callback(result)
            }

            AttachmentTypeEnum.Video.value -> {
                resizingAttachmentsMap[attachment.messageTid] = attachment.messageTid.toString()
                transcodeVideo(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    progressCallback = {
                        if (pausedTasksMap[attachment.messageTid] == null)
                            task.preparingCallback?.onPreparing(attachment.toTransferData(Preparing, it.progressPercent))
                    }) {
                    resizingAttachmentsMap.remove(attachment.messageTid)
                    callback(it)
                }
            }

            else -> callback.invoke(Result.success(null))
        }
    }

    private fun removeFromSharingPath(filePath: String?) {
        val current = sharingFilesPath.firstOrNull { it.originalPath == filePath }
                ?: return
        sharingFilesPath.removeAll {
            it.originalPath == current.originalPath
        }
        sharingFilesPath.remove(current)
    }

    @Synchronized
    private fun getAppropriateTasks(transferTask: TransferTask): List<TransferTask> {
        //Get current task original path with resized path
        val currentTaskOriginalPath = sharingFilesPath.firstOrNull {
            it.originalPath == transferTask.attachment.originalFilePath
        }?.originalPath

        //Find all tasks with the same original file path, to update transfer state
        val tasks = sharingFilesPath.filter {
            it.originalPath == currentTaskOriginalPath
        }
        return fileTransferService.getTasks().values.filter {
            tasks.any { data -> data.messageTid == it.attachment.messageTid }
                    && it.state != PauseUpload
        }
    }

    private fun getAttachmentChecksum(filePath: String?): FileChecksumData? {
        if (!SceytChatUIKit.config.preventDuplicateAttachmentUpload) return null
        val data: FileChecksumData?
        runBlocking(Dispatchers.IO) {
            data = attachmentLogic.getFileChecksumData(filePath)
        }
        return data
    }

    private fun checkMaybeAlreadyUploadedWithAnotherMessage(
            checksumData: FileChecksumData?,
            task: TransferTask,
    ): Pair<Boolean, String?> {
        checksumData ?: return false to ""
        if (checksumData.url.isNotNullOrBlank()) {
            if (!checksumData.resizedFilePath.isNullOrEmpty())
                task.updateFileLocationCallback?.onUpdateFileLocation(checksumData.resizedFilePath)

            return true to checksumData.url
        }

        return false to ""
    }

    private fun getAttachmentThumbPath(
            context: Context,
            attachment: SceytAttachment,
            size: Size,
    ): Result<String?> {
        val path = attachment.filePath ?: return Result.failure(FileNotFoundException())
        val minSize = max(size.height, size.width)
        val reqSize = if (minSize > 0) minSize.toFloat() else 800f
        val resizePath = when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                FileResizeUtil.getImageThumbAsFile(context, path, reqSize)?.path
            }

            AttachmentTypeEnum.Video.value -> {
                FileResizeUtil.getVideoThumbAsFile(context, path, reqSize)?.path
            }

            else -> null
        }
        return Result.success(resizePath)
    }

    private fun getPreparingThumbKey(attachment: SceytAttachment, data: ThumbData): String {
        val path = if (attachment.originalFilePath.isNullOrBlank())
            attachment.filePath else attachment.originalFilePath
        return "$path${data.size}"
    }

    private fun Context.getSaveFileLocationRoot(type: String): File {
        return when (type) {
            AttachmentTypeEnum.Image.value -> File(filesDir, SceytConstants.ImageFilesDirName)
            AttachmentTypeEnum.Video.value -> File(filesDir, SceytConstants.VideoFilesDirName)
            else -> File(filesDir, SceytConstants.FileFilesDirName)
        }.apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getDestinationFile(context: Context, attachment: SceytAttachment): File {
        val root = context.getSaveFileLocationRoot(attachment.type)
        val fileName = attachment.name.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val destinationFile = getOrCreateUniqueFileDirectory(root, fileName)
        return destinationFile
    }

    private val SceytAttachment.downloadMapKey: String
        get() = url + messageTid

    fun clear() {
        pausedTasksMap.clear()
        downloadingUrlMap.clear()
        sharingFilesPath.clear()
        preparingThumbsMap.clear()
    }

    data class ThumbPathsData(
            val messageTid: Long,
            val path: String,
            val size: Size,
    )

    data class ShareFilesData(
            val originalPath: String,
            val messageTid: Long,
    )
}