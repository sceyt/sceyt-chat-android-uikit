package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Log
import android.util.Size
import com.koushikdutta.ion.Ion
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
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
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.checkLoadedFileIsCorrect
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max

internal class FileTransferLogicImpl(
        private val context: Context,
        private val attachmentLogic: PersistenceAttachmentLogic
) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, ThumbPathsData>()
    private var preparingThumbsMap = hashMapOf<Long, Long>()
    private var pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private var currentUploadingAttachment: SceytAttachment? = null
    private var pausedTasksMap = hashMapOf<Long, Long>()
    private var resizingAttachmentsMap = hashMapOf<String, String>()

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
                        uploadAttachment = uploadAttachment.copy(filePath = path)
                    }
                } else SceytLog.i("resizeResult", "Couldn't resize sharing file with reason ${it.exceptionOrNull()}")

                uploadSharedAttachment(uploadAttachment, task)
            }
        }
        sharingFilesPath.add(data)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        val loadedFile = File(context.getSaveFileLocation(attachment.type),
            "${attachment.messageTid}_${attachment.name}")
        val file = attachment.checkLoadedFileIsCorrect(loadedFile)

        if (file != null) {
            task.downloadCallback?.onResult(SceytResponse.Success(file.path))
        } else {
            val downloadMapKey = attachment.url + attachment.messageTid
            if (downloadingUrlMap[downloadMapKey] != null) return

            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            task.progressCallback?.onProgress(TransferData(
                task.messageTid, 0f, Downloading, null, attachment.url))
            downloadingUrlMap[downloadMapKey] = downloadMapKey

            Ion.with(context)
                .load(attachment.url)
                .progress { downloaded, total ->
                    if (pausedTasksMap[attachment.messageTid] == null) {
                        val progress = ((downloaded / total.toFloat())) * 100
                        task.progressCallback?.onProgress(TransferData(
                            task.messageTid, progress, Downloading, null, attachment.url))
                    }
                }
                .write(loadedFile)
                .setCallback { e, result ->
                    if (result == null && e != null) {
                        loadedFile.delete()
                        task.downloadCallback?.onResult(SceytResponse.Error(SceytException(0, e.message)))
                    } else
                        task.downloadCallback?.onResult(SceytResponse.Success(result.path))

                    downloadingUrlMap.remove(downloadMapKey)
                }
        }
    }

    override fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        pausedTasksMap[attachment.messageTid] = attachment.messageTid
        if (attachment.type == AttachmentTypeEnum.Video.value)
            VideoTranscodeHelper.cancel(attachment.filePath)

        when (state) {
            PendingUpload, Uploading, Preparing, FilePathChanged, WaitingToUpload -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = PauseUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
                }
                //todo
                uploadNext()

            }

            PendingDownload, Downloading -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = PauseUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
                }
                //todo
            }

            else -> {}
        }
    }

    override fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        pausedTasksMap.remove(attachment.messageTid)
        when (state) {
            PendingDownload, PauseDownload, ErrorDownload -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    pausedTasksMap.remove(attachment.messageTid)
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(Downloading))
                    downloadFile(attachment, it)
                }
            }

            PendingUpload, PauseUpload, ErrorUpload -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = WaitingToUpload
                    it.resumePauseCallback?.onResumePause(attachment.toTransferData(WaitingToUpload))
                    uploadFile(attachment, it)
                    //Todo need implement resume sharing files
                }
            }

            else -> return
        }
    }

    override fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        attachment.filePath ?: return
        val size = thumbData.size
        val thumbKey = getPreparingThumbKey(attachment, thumbData)
        if (preparingThumbsMap[messageTid] != null) return
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val readyThumb = thumbPaths[thumbKey]
        if (readyThumb != null) {
            task.thumbCallback?.onThumb(readyThumb.path, thumbData)
            return
        } else {
            preparingThumbsMap[messageTid] = messageTid
            val result = getAttachmentThumbPath(context, attachment, size)
            if (result.isSuccess) {
                result.getOrNull()?.let { path ->
                    thumbPaths[thumbKey] = ThumbPathsData(messageTid, path, size)
                    task.thumbCallback?.onThumb(path, thumbData)
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

    private fun getPreparingThumbKey(attachment: SceytAttachment, data: ThumbData): String {
        val path = if (attachment.originalFilePath.isNullOrBlank())
            attachment.filePath else attachment.originalFilePath
        return "$path${data.size}"
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
                    uploadAttachment = uploadAttachment.copy(filePath = path)
                }
            } else SceytLog.i("resizeResult", "Couldn't resize file with reason ${it.exceptionOrNull()}")

            ChatClient.getClient().upload(uploadAttachment.filePath, object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f) return
                    transferTask.progressCallback?.onProgress(TransferData(transferTask.messageTid,
                        progress * 100, Uploading, uploadAttachment.filePath, null))
                }

                override fun onError(exception: SceytException?) {
                    Log.e(TAG, "Error upload file ${exception?.message}")
                    transferTask.uploadResultCallback?.onResult(SceytResponse.Error(exception))
                }
            }, object : UrlCallback {
                override fun onResult(p0: String?) {
                    transferTask.uploadResultCallback?.onResult(SceytResponse.Success(p0))
                    uploadNext()
                }

                override fun onError(exception: SceytException?) {
                    Log.e(TAG, "Error upload file ${exception?.message}")
                    transferTask.uploadResultCallback?.onResult(SceytResponse.Error(exception))
                    uploadNext()
                }
            })
        }
    }

    private fun uploadSharedAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
            override fun onResult(progress: Float) {
                if (progress == 1f || pausedTasksMap[attachment.messageTid] != null) return
                getAppropriateTasks(transferTask).forEach { task ->
                    fileTransferService.getTasks()[task.messageTid.toString()]?.state = Uploading
                    task.progressCallback?.onProgress(TransferData(task.messageTid,
                        progress * 100, Uploading, task.attachment.filePath, null))
                }
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.uploadResultCallback?.onResult(SceytResponse.Error(exception))
                }
                removeFromSharingPath(attachment.originalFilePath)
            }
        }, object : UrlCallback {
            override fun onResult(p0: String?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.uploadResultCallback?.onResult(SceytResponse.Success(p0))
                }
                removeFromSharingPath(attachment.originalFilePath)
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.uploadResultCallback?.onResult(SceytResponse.Error(exception))
                }
                removeFromSharingPath(attachment.originalFilePath)
            }
        })
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

    private fun checkAndResizeMessageAttachments(
            context: Context,
            attachment: SceytAttachment,
            checksumData: FileChecksumData?,
            task: TransferTask, callback: (Result<String?>) -> Unit
    ) {

        val path = checksumData?.resizedFilePath
        if (path != null && File(path).exists()) {
            callback(Result.success(path))
            return
        }
        when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                resizingAttachmentsMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
                val reqSize = SceytChatUIKit.config.imageAttachmentResizeConfig.dimensionThreshold
                val quality = SceytChatUIKit.config.imageAttachmentResizeConfig.compressionQuality
                val result = resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = reqSize,
                    quality = quality
                )
                resizingAttachmentsMap.remove(attachment.messageTid.toString())
                callback(result)
            }

            AttachmentTypeEnum.Video.value -> {
                resizingAttachmentsMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
                transcodeVideo(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    progressCallback = {
                        if (pausedTasksMap[attachment.messageTid] == null)
                            task.preparingCallback?.onPreparing(attachment.toTransferData(Preparing, it.progressPercent))
                    }) {
                    resizingAttachmentsMap.remove(attachment.messageTid.toString())
                    callback(it)
                }
            }

            else -> callback.invoke(Result.success(null))
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
            task: TransferTask
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
            size: Size
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

    private fun Context.getSaveFileLocation(type: String): File {
        return when (type) {
            AttachmentTypeEnum.Image.value -> File(filesDir, SceytConstants.ImageFilesDirName)
            AttachmentTypeEnum.Video.value -> File(filesDir, SceytConstants.VideoFilesDirName)
            else -> File(filesDir, SceytConstants.FileFilesDirName)
        }.apply {
            if (!exists()) mkdirs()
        }
    }

    fun clear() {
        pausedTasksMap.clear()
        downloadingUrlMap.clear()
        sharingFilesPath.clear()
        preparingThumbsMap.clear()
    }

    data class ThumbPathsData(
            val messageTid: Long,
            val path: String,
            val size: Size
    )

    data class ShareFilesData(
            val originalPath: String,
            val messageTid: Long
    )
}