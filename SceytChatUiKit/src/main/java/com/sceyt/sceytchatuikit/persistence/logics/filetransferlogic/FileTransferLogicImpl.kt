package com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic

import android.content.Context
import android.util.Log
import android.util.Size
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.koushikdutta.ion.Ion
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.persistence.extensions.transcodeVideo
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferTask
import com.sceyt.sceytchatuikit.presentation.common.checkLoadedFileIsCorrect
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max

internal class FileTransferLogicImpl(private val context: Context) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, ThumbPathsData>()
    private var preparingThumbsMap = hashMapOf<String, Long>()
    private var pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private var currentUploadingAttachment: SceytAttachment? = null
    private var pausedTasksMap = hashMapOf<String, String>()
    private var resizingAttachmentsMap = hashMapOf<String, String>()

    private var sharingFilesPath = Collections.synchronizedSet<ShareFilesData>(mutableSetOf())

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        /*
        //todo Uncomment this logic after implementing play/pause logic
        if (attachment.transferState == TransferState.PauseUpload) {
             pausedTasksMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
             return
         }*/
        checkAndUpload(attachment, task)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        fileTransferService.getTasks()[task.messageTid.toString()] = task
        val data = ShareFilesData(attachment.filePath.toString(), attachment.filePath.toString(), attachment.messageTid)
        if (sharingFilesPath.none { it.originalPath == attachment.filePath }) {
            checkAndResizeMessageAttachments(context, attachment) {
                if (it.isSuccess) {
                    it.getOrNull()?.let { path ->
                        task.updateFileLocationCallback.onUpdateFileLocation(path)
                        data.resizedPath = path
                    }
                } else Log.i("resizeResult", "Couldn't resize sharing file with reason ${it.exceptionOrNull()}")

                uploadSharedAttachment(attachment, task)
            }
        }
        sharingFilesPath.add(data)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        val loadedFile = File(getSaveFileLocation(attachment), "${attachment.messageTid}_${attachment.name}")
        val file = attachment.checkLoadedFileIsCorrect(loadedFile)

        if (file != null) {
            task.resultCallback.onResult(SceytResponse.Success(file.path))
        } else {
            if (downloadingUrlMap[attachment.url] != null) return
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            task.progressCallback.onProgress(TransferData(
                task.messageTid, 0f, TransferState.Downloading, null, attachment.url))
            attachment.url?.let { url ->
                downloadingUrlMap[url] = url
                Ion.with(context)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        if (pausedTasksMap[attachment.url.toString()] == null) {
                            val progress = ((downloaded / total.toFloat())) * 100
                            task.progressCallback.onProgress(TransferData(
                                task.messageTid, progress, TransferState.Downloading, null, attachment.url))
                        }
                    }
                    .write(loadedFile)
                    .setCallback { e, result ->
                        if (result == null && e != null) {
                            loadedFile.delete()
                            task.resultCallback.onResult(SceytResponse.Error(SceytException(0, e.message)))
                        } else
                            task.resultCallback.onResult(SceytResponse.Success(result.path))

                        downloadingUrlMap.remove(attachment.url)
                    }
            }
        }
    }

    override fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        pausedTasksMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
        if (attachment.type == AttachmentTypeEnum.Video.value())
            VideoCompressor.cancel()

        when (state) {
            TransferState.PendingUpload, TransferState.Uploading -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.state = TransferState.PauseUpload
                //todo
                uploadNext()

            }

            TransferState.PendingDownload, TransferState.Downloading -> {
                //todo
            }

            else -> {}
        }
    }

    override fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        pausedTasksMap.remove(attachment.messageTid.toString())
        when (state) {
            TransferState.PendingDownload, TransferState.PauseDownload, TransferState.ErrorDownload -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    downloadingUrlMap.remove(attachment.messageTid.toString())
                    downloadFile(attachment, it)
                }
            }

            TransferState.PendingUpload, TransferState.PauseUpload, TransferState.ErrorUpload -> {
                fileTransferService.getTasks()[attachment.messageTid.toString()]?.let {
                    it.state = TransferState.Uploading
                    uploadFile(attachment, it)
                    //Todo need implement resume sharing files
                }
            }

            else -> return
        }
    }

    override fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        val size = thumbData.size
        val thumbKey = getPreparingThumbKey(messageTid, size)
        if (preparingThumbsMap[thumbKey] != null) return
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val readyThumb = thumbPaths[thumbKey]
        if (readyThumb != null) {
            task.thumbCallback.onThumb(readyThumb.path, thumbData)
            return
        } else {
            preparingThumbsMap[thumbKey] = messageTid
            val result = getAttachmentThumbPath(context, attachment, size)
            if (result.isSuccess)
                result.getOrNull()?.let { path ->
                    thumbPaths[thumbKey] = ThumbPathsData(messageTid, path, size)
                    task.thumbCallback.onThumb(path, thumbData)
                }
        }
        preparingThumbsMap.remove(thumbKey)
    }

    override fun clearPreparingThumbPaths() {
        preparingThumbsMap.clear()
    }

    private fun getPreparingThumbKey(messageTid: Long, size: Size) = "$messageTid$size"

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
        pendingUploadQueue.poll()?.let {
            uploadAttachment(it.first, it.second)
        }
    }

    private fun uploadAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        currentUploadingAttachment = attachment
        checkAndResizeMessageAttachments(context, attachment) {
            // Check if task was paused
            if (pausedTasksMap[attachment.messageTid.toString()] != null) {
                uploadNext()
                return@checkAndResizeMessageAttachments
            }

            if (it.isSuccess) {
                it.getOrNull()?.let { path ->
                    transferTask.updateFileLocationCallback.onUpdateFileLocation(path)
                }
            } else Log.i("resizeResult", "Couldn't resize file with reason ${it.exceptionOrNull()}")

            ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f) return
                    transferTask.progressCallback.onProgress(TransferData(transferTask.messageTid,
                        progress * 100, TransferState.Uploading, attachment.filePath, null))
                }

                override fun onError(exception: SceytException?) {
                    transferTask.resultCallback.onResult(SceytResponse.Error(exception))
                }
            }, object : UrlCallback {
                override fun onResult(p0: String?) {
                    transferTask.resultCallback.onResult(SceytResponse.Success(p0))
                    uploadNext()
                }

                override fun onError(exception: SceytException?) {
                    transferTask.resultCallback.onResult(SceytResponse.Error(exception))
                    uploadNext()
                }
            })
        }
    }

    private fun uploadSharedAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        fun removeFromSharingPath() {
            val current = sharingFilesPath.firstOrNull { it.resizedPath == attachment.filePath }
                    ?: return
            sharingFilesPath.removeAll {
                it.originalPath == current.originalPath
            }
            sharingFilesPath.remove(current)
        }

        ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
            override fun onResult(progress: Float) {
                if (progress == 1f || pausedTasksMap[attachment.messageTid.toString()] != null) return
                getAppropriateTasks(transferTask).forEach { task ->
                    fileTransferService.getTasks()[task.messageTid.toString()]?.state = TransferState.Uploading
                    task.progressCallback.onProgress(TransferData(task.messageTid,
                        progress * 100, TransferState.Uploading, task.attachment.filePath, null))
                }
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Error(exception))
                }
                removeFromSharingPath()
            }
        }, object : UrlCallback {
            override fun onResult(p0: String?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Success(p0))
                }
                removeFromSharingPath()
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Error(exception))
                }
                removeFromSharingPath()
            }
        })
    }

    @Synchronized
    private fun getAppropriateTasks(transferTask: TransferTask): List<TransferTask> {
        //Get current task original path with resized path
        val currentTaskOriginalPath = sharingFilesPath.firstOrNull {
            it.resizedPath == transferTask.attachment.filePath
        }?.originalPath

        //Find all tasks with the same original file path, to update transfer state
        val tasks = sharingFilesPath.filter {
            it.originalPath == currentTaskOriginalPath
        }
        return fileTransferService.getTasks().values.filter {
            tasks.any { data -> data.messageTid == it.attachment.messageTid }
                    && it.state != TransferState.PauseUpload
        }
    }

    private fun checkAndResizeMessageAttachments(context: Context, attachment: SceytAttachment, callback: (Result<String?>) -> Unit) {
        when (attachment.type) {
            AttachmentTypeEnum.Image.value() -> {
                resizingAttachmentsMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
                val result = resizeImage(context, attachment.filePath, 1080)
                callback(result)
                resizingAttachmentsMap.remove(attachment.messageTid.toString())
            }

            AttachmentTypeEnum.Video.value() -> {
                resizingAttachmentsMap[attachment.messageTid.toString()] = attachment.messageTid.toString()
                transcodeVideo(context, attachment.filePath) {
                    callback(it)
                    resizingAttachmentsMap.remove(attachment.messageTid.toString())
                }
            }

            else -> callback.invoke(Result.success(null))
        }
    }

    private fun getAttachmentThumbPath(context: Context, attachment: SceytAttachment, size: Size): Result<String?> {
        val path = attachment.filePath ?: return Result.failure(FileNotFoundException())
        val minSize = max(size.height, size.width)
        val reqSize = if (minSize > 0) minSize.toFloat() else 800f
        val resizePath = when (attachment.type) {
            AttachmentTypeEnum.Image.value() -> {
                FileResizeUtil.getImageThumbAsFile(context, path, reqSize)?.path
            }

            AttachmentTypeEnum.Video.value() -> {
                FileResizeUtil.getVideoThumbAsFile(context, path, reqSize)?.path
            }

            else -> null
        }
        return Result.success(resizePath)
    }

    private fun getSaveFileLocation(attachment: SceytAttachment): File {
        return when (attachment.type) {
            AttachmentTypeEnum.Image.value() -> File(context.filesDir, "Sceyt Images")
            AttachmentTypeEnum.Video.value() -> File(context.filesDir, "Sceyt Videos")
            else -> File(context.filesDir, "Sceyt Files")
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

    data class ThumbPathsData(val messageTid: Long,
                              val path: String,
                              val size: Size)

    data class ShareFilesData(
            val originalPath: String,
            var resizedPath: String,
            val messageTid: Long
    )
}