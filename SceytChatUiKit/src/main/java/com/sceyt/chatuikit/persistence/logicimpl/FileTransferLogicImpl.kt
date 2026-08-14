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
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.transcodeVideo
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
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
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.io.File
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class FileTransferLogicImpl(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val thumbPathResolver: ThumbPathResolver,
) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val transferUtility by lazy { FileTransferUtility() }
    private val videoThumbUploader by lazy {
        VideoThumbUploader(context, attachmentLogic, transferUtility)
    }
    private val downloadingUrlKeys = ConcurrentHashMap.newKeySet<String>()
    private val thumbPaths = ConcurrentHashMap<String, ThumbPathsData>()
    private val preparingThumbs = ConcurrentHashMap<String, ThumbData>()
    private val pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private val uploadQueueLock = Any()
    private var currentUploadingAttachment: SceytAttachment? = null
    private val pausedTaskIds = ConcurrentHashMap.newKeySet<Long>()
    private val resizingAttachmentIds = ConcurrentHashMap.newKeySet<Long>()
    private val sharingFilesPath = ConcurrentHashMap.newKeySet<ShareFilesData>()

    companion object {
        private const val TAG = "FileTransferLogic"
        private const val VIDEO_THUMB_FILE_NAME = "video_thumb.jpeg"
        private val EMPTY_THUMB_SIZE = Size(0, 0)
    }

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        checkAndUpload(attachment, task)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        fileTransferService.addTransferTask(task)
        val data = ShareFilesData(attachment.originalFilePath.toString(), attachment.messageTid)
        if (sharingFilesPath.none { it.originalPath == attachment.originalFilePath }) {
            val checksum = getAttachmentChecksum(attachment.originalFilePath)

            // Extract and upload the video first frame in parallel with the video itself
            videoThumbUploader.start(attachment)

            val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)
            if (uploaded && url != null) {
                sharingFilesPath.add(data)
                deliverSharedUploadResult(attachment, task, SceytResponse.Success(url))
                return
            }

            var uploadAttachment = attachment
            checkAndResizeMessageAttachments(context, attachment, checksum, task) { result ->
                result.onSuccess { path ->
                    task.updateFileLocationCallback?.onUpdateFileLocation(path)
                    uploadAttachment = uploadAttachment.copy(
                        filePath = path,
                        fileSize = getFileSize(path)
                    )

                }.onFailure {
                    SceytLog.i(TAG, "Couldn't resize sharing file with reason ${it.message}")
                }

                uploadSharedAttachment(uploadAttachment, task)
            }
        }
        sharingFilesPath.add(data)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        // The uploaded video thumb is much smaller, so it's shown while the video is downloading
        downloadVideoThumb(attachment, task)

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
            if (!downloadingUrlKeys.add(downloadMapKey)) return
            pausedTaskIds.remove(attachment.messageTid)

            task.progressCallback?.onProgress(
                TransferData(
                    messageTid = task.messageTid,
                    progressPercent = attachment.progressPercent ?: 0f,
                    state = Downloading,
                    filePath = attachment.filePath,
                    url = attachment.url
                )
            )

            transferUtility.downloadFile(
                attachment = attachment,
                destFile = destFile,
                onProgress = { progressPercent ->
                    if (!pausedTaskIds.contains(attachment.messageTid)) {
                        task.progressCallback?.onProgress(
                            TransferData(
                                task.messageTid, progressPercent, Downloading, null, attachment.url
                            )
                        )
                    }
                },
                onResult = {
                    if (it is SceytResponse.Error)
                        destFile.delete()

                    task.downloadCallback?.onResult(it)
                    downloadingUrlKeys.remove(downloadMapKey)
                })
        }
    }

    override fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, Uploading, Preparing, FilePathChanged, WaitingToUpload -> {
                pausedTaskIds.add(attachment.messageTid)
                if (attachment.type == AttachmentTypeEnum.Video.value) {
                    VideoTranscodeHelper.cancel(attachment.filePath)
                    videoThumbUploader.cancel(attachment)
                }

                fileTransferService.findTransferTask(attachment)?.let {
                    it.state = PauseUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
                }

                transferUtility.pauseUpload(attachment)
                uploadNext()
            }

            PendingDownload, Downloading -> {
                pausedTaskIds.add(attachment.messageTid)
                fileTransferService.findTransferTask(attachment)?.let {
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
                pausedTaskIds.remove(attachment.messageTid)
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
                    if (!resizingAttachmentIds.contains(attachment.messageTid)) {
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
                pausedTaskIds.remove(attachment.messageTid)
                val destFile = getDestinationFile(context, attachment)
                val file = attachment.isAttachmentExistAndFullyLoaded(destFile)

                if (file != null) {
                    fileTransferService.findTransferTask(attachment)?.downloadCallback?.onResult(
                        SceytResponse.Success(file.path)
                    )
                } else {
                    if (!transferUtility.resumeDownload(attachment)) {
                        downloadingUrlKeys.remove(attachment.downloadMapKey)
                        downloadFile(
                            attachment,
                            fileTransferService.findOrCreateTransferTask(attachment)
                        )
                    }

                    fileTransferService.findTransferTask(attachment)?.resumePauseCallback?.onResumePause(
                        attachment.toTransferData(Downloading)
                    )
                }
            }

            else -> return
        }
    }

    override fun getAttachmentThumb(
        messageTid: Long,
        attachment: SceytAttachment,
        data: ThumbData
    ) {
        val size = data.size
        val thumbKey = getThumbSourceKey(attachment, data)
        val preparingThumbKey = "${attachment.messageTid}_${thumbKey}_${data.key}"

        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val readyThumb = thumbPaths[thumbKey]
        if (readyThumb != null) {
            task.thumbCallback?.onThumb(readyThumb.path, data)
            return
        }

        // The uploaded video thumb is already downloaded, so there is no need
        // to generate a thumb from the video file
        getDownloadedVideoThumbFile(attachment)?.let {
            task.thumbCallback?.onThumb(it.path, data)
            return
        }

        attachment.filePath ?: return
        if (preparingThumbs.put(preparingThumbKey, data) != null) return
        thumbPathResolver.getThumbPath(context, attachment, size).onSuccess { path ->
            thumbPaths[thumbKey] = ThumbPathsData(messageTid, path, size)
            task.thumbCallback?.onThumb(path, preparingThumbs.remove(preparingThumbKey) ?: data)
        }.onFailure {
            preparingThumbs.remove(preparingThumbKey)
            SceytLog.e(
                TAG, "Couldn't get a thumb for messageTid: $messageTid," +
                        " path:${attachment.filePath} with reason ${it.message}"
            )
        }
    }

    /**
     * Downloads the video thumb, which was uploaded with the video, to show the video preview
     * without waiting for the video itself. The thumb is downloaded with the same flow as any
     * other attachment, so a custom download implementation is used as well.
     */
    private fun downloadVideoThumb(attachment: SceytAttachment, task: TransferTask) {
        if (attachment.filePath.isNotNullOrBlank()) return
        val thumbAttachment = attachment.videoThumbAttachment ?: return
        val destFile = getDestinationFile(context, thumbAttachment)

        if (destFile.exists() && destFile.length() > 0L) {
            notifyVideoThumbLoaded(task, destFile.path)
            return
        }

        val downloadMapKey = thumbAttachment.downloadMapKey
        if (!downloadingUrlKeys.add(downloadMapKey)) return

        transferUtility.downloadFile(
            attachment = thumbAttachment,
            destFile = destFile,
            onProgress = {},
            onResult = { response ->
                downloadingUrlKeys.remove(downloadMapKey)
                if (response is SceytResponse.Success) {
                    notifyVideoThumbLoaded(task, destFile.path)
                } else {
                    destFile.delete()
                    SceytLog.e(
                        TAG, "Couldn't download the video thumb for messageTid:" +
                                " ${attachment.messageTid}, reason: ${response.message}"
                    )
                }
            })
    }

    /** Notifies every screen, which shows the attachment, about the loaded thumb. */
    private fun notifyVideoThumbLoaded(task: TransferTask, path: String) {
        ThumbFor.entries.forEach { thumbFor ->
            task.thumbCallback?.onThumb(path, ThumbData(thumbFor.value, null, EMPTY_THUMB_SIZE))
        }
    }

    private fun getDownloadedVideoThumbFile(attachment: SceytAttachment): File? {
        val thumbAttachment = attachment.videoThumbAttachment ?: return null
        return getDestinationFile(context, thumbAttachment).takeIf {
            it.exists() && it.length() > 0L
        }
    }

    /** Image attachment, which points to the uploaded video thumb, to download it as a usual file. */
    private val SceytAttachment.videoThumbAttachment: SceytAttachment?
        get() {
            val thumbUrl = getVideoThumbUrl()?.takeIf { it.isNotBlank() } ?: return null
            return copy(
                name = VIDEO_THUMB_FILE_NAME,
                type = AttachmentTypeEnum.Image.value,
                url = thumbUrl,
                filePath = null,
                originalFilePath = null,
                metadata = null,
                fileSize = 0
            )
        }

    override fun clearPreparingThumbPaths() {
        preparingThumbs.clear()
    }

    private fun checkAndUpload(attachment: SceytAttachment, task: TransferTask) {
        val shouldUpload = synchronized(uploadQueueLock) {
            if (currentUploadingAttachment == null) {
                currentUploadingAttachment = attachment
                true
            } else {
                val alreadyExist =
                    currentUploadingAttachment?.messageTid == attachment.messageTid ||
                            pendingUploadQueue.any { it.first.messageTid == attachment.messageTid }

                if (!alreadyExist)
                    pendingUploadQueue.add(Pair(attachment, task))
                false
            }
        }
        if (shouldUpload)
            uploadAttachment(attachment, task)
    }

    private fun uploadNext() {
        val nextUpload = synchronized(uploadQueueLock) {
            currentUploadingAttachment = null
            pendingUploadQueue.poll()?.also { (attachment) ->
                currentUploadingAttachment = attachment
            }
        } ?: return

        uploadAttachment(nextUpload.first, nextUpload.second)
    }

    private fun uploadAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        // Extract and upload the video first frame in parallel with the video itself
        videoThumbUploader.start(attachment)

        val checksum = getAttachmentChecksum(attachment.originalFilePath)

        val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, transferTask)
        if (uploaded && url != null) {
            uploadNext()
            deliverUploadResult(attachment, transferTask, SceytResponse.Success(url))
            return
        }
        var uploadAttachment = attachment
        checkAndResizeMessageAttachments(context, attachment, checksum, transferTask) { result ->
            // Check if task was paused
            if (pausedTaskIds.contains(attachment.messageTid)) {
                uploadNext()
                return@checkAndResizeMessageAttachments
            }
            result.onSuccess { path ->
                transferTask.updateFileLocationCallback?.onUpdateFileLocation(path)
                uploadAttachment = uploadAttachment.copy(
                    filePath = path,
                    fileSize = getFileSize(path)
                )
            }.onFailure {
                SceytLog.i(TAG, "Couldn't resize file with reason ${it.message}")
            }

            if (!transferUtility.resumeUpload(attachment)) {
                transferUtility.uploadFile(
                    uploadAttachment,
                    onProgress = { progressPercent ->
                        transferTask.progressCallback?.onProgress(
                            TransferData(
                                transferTask.messageTid,
                                progressPercent, Uploading, uploadAttachment.filePath, null
                            )
                        )
                    },
                    onResult = { response ->
                        uploadNext()
                        deliverUploadResult(uploadAttachment, transferTask, response)
                    })
            }
        }
    }

    /**
     * Delivers the upload result, waiting for the video thumb upload result if the attachment
     * is a video, which doesn't have an uploaded thumb yet.
     */
    private fun deliverUploadResult(
        attachment: SceytAttachment,
        transferTask: TransferTask,
        response: SceytResponse<String>,
    ) {
        videoThumbUploader.deliverResult(
            attachment = attachment,
            tasks = listOf(transferTask),
            response = response,
            isPaused = pausedTaskIds::contains
        )
    }

    private fun deliverSharedUploadResult(
        attachment: SceytAttachment,
        transferTask: TransferTask,
        response: SceytResponse<String>,
    ) {
        videoThumbUploader.deliverResult(
            attachment = attachment,
            tasks = getAppropriateTasks(transferTask),
            response = response,
            isPaused = pausedTaskIds::contains,
            onDelivered = { removeFromSharingPath(attachment.originalFilePath) }
        )
    }

    private fun uploadSharedAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        transferUtility.uploadFile(
            attachment = attachment,
            onProgress = { progressPercent ->
                if (pausedTaskIds.contains(attachment.messageTid)) return@uploadFile
                getAppropriateTasks(transferTask).forEach { task ->
                    task.state = Uploading
                    task.progressCallback?.onProgress(
                        TransferData(
                            task.messageTid,
                            progressPercent, Uploading, task.attachment.filePath, null
                        )
                    )
                }
            },
            onResult = { response ->
                deliverSharedUploadResult(attachment, transferTask, response)
            })
    }

    private fun checkAndResizeMessageAttachments(
        context: Context,
        attachment: SceytAttachment,
        checksumData: FileChecksumData?,
        task: TransferTask, callback: (Result<String>) -> Unit,
    ) {

        val path = checksumData?.resizedFilePath
        if (path != null && File(path).exists()) {
            callback(Result.success(path))
            return
        }
        when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                resizingAttachmentIds.add(attachment.messageTid)
                val reqSize = SceytChatUIKit.config.imageAttachmentResizeConfig.dimensionThreshold
                val quality = SceytChatUIKit.config.imageAttachmentResizeConfig.compressionQuality
                val result = resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = reqSize,
                    quality = quality
                )
                resizingAttachmentIds.remove(attachment.messageTid)
                callback(result)
            }

            AttachmentTypeEnum.Video.value -> {
                resizingAttachmentIds.add(attachment.messageTid)
                transcodeVideo(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    progressCallback = {
                        if (!pausedTaskIds.contains(attachment.messageTid))
                            task.preparingCallback?.onPreparing(
                                attachment.toTransferData(
                                    Preparing,
                                    it.progressPercent
                                )
                            )
                    }) {
                    resizingAttachmentIds.remove(attachment.messageTid)
                    callback(it)
                }
            }

            else -> callback.invoke(Result.failure(Exception("Unsupported attachment type: ${attachment.type}")))
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
        // The video could be already uploaded by a previous attempt, which failed to upload its thumb
        task.attachment.url?.takeIf {
            it.isNotBlank() && task.attachment.type == AttachmentTypeEnum.Video.value
        }?.let { return true to it }

        checksumData ?: return false to ""
        if (checksumData.url.isNotNullOrBlank()) {
            if (!checksumData.resizedFilePath.isNullOrEmpty())
                task.updateFileLocationCallback?.onUpdateFileLocation(checksumData.resizedFilePath)

            return true to checksumData.url
        }

        return false to ""
    }

    private fun getThumbSourceKey(attachment: SceytAttachment, data: ThumbData): String {
        val path = if (attachment.originalFilePath.isNullOrBlank())
            attachment.filePath ?: data.filePath ?: attachment.messageTid.toString()
        else attachment.originalFilePath
        return "${path}_${data.size}"
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
        val childDir = File(root, attachment.messageTid.toString())
        if (!childDir.exists()) {
            childDir.mkdirs()
        }
        val name = attachment.name.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val destinationFile = File(childDir, name)
        return destinationFile
    }

    private val SceytAttachment.downloadMapKey: String
        get() = url + messageTid

    fun clear() {
        videoThumbUploader.clear()
        pausedTaskIds.clear()
        downloadingUrlKeys.clear()
        resizingAttachmentIds.clear()
        sharingFilesPath.clear()
        preparingThumbs.clear()
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
