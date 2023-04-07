package com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic

import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Size
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
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferTask
import com.sceyt.sceytchatuikit.presentation.common.checkLoadedFileIsCorrect
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.math.max

internal class FileTransferLogicImpl(private val application: Application) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, ThumbPathsData>()
    private var preparingThumbsMap = hashMapOf<String, Long>()
    private var pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private var currentUploadingAttachment: SceytAttachment? = null
    private var pausedTasksMap = hashMapOf<String, String>()

    private var sharedFilesPath = Collections.synchronizedSet<String>(mutableSetOf())

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        checkAndUpload(attachment, task)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        fileTransferService.getTasks()[task.messageTid.toString()] = task
        if (!sharedFilesPath.contains(attachment.filePath)) {
            sharedFilesPath.add(attachment.filePath.toString())
            uploadSharedAttachment(attachment, task)
        }
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        val loadedFile = File(application.filesDir, attachment.messageTid.toString())
        val file = attachment.checkLoadedFileIsCorrect(loadedFile)

        if (file != null) {
            task.resultCallback.onResult(SceytResponse.Success(file.path))
        } else {
            if (downloadingUrlMap[attachment.url] != null) return
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            task.progressCallback.onProgress(TransferData(
                task.messageTid, attachment.tid, 0f, TransferState.Downloading, null, attachment.url))
            attachment.url?.let { url ->
                downloadingUrlMap[url] = url
                Ion.with(application)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        if (pausedTasksMap[attachment.url.toString()] == null) {
                            val progress = ((downloaded / total.toFloat())) * 100
                            task.progressCallback.onProgress(TransferData(
                                task.messageTid, attachment.tid, progress, TransferState.Downloading, null, attachment.url))
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

    override fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, size: Size) {
        val thumbKey = getPreparingThumbKey(messageTid, size)
        if (preparingThumbsMap[thumbKey] != null) return
        preparingThumbsMap[thumbKey] = messageTid
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val readyThumb = thumbPaths[messageTid.toString()]
        if (readyThumb != null && readyThumb.size == size) {
            task.thumbCallback.onThumb(readyThumb.path)
            preparingThumbsMap.remove(thumbKey)
            return
        } else {
            val result = getAttachmentThumbPath(application, attachment, size)
            if (result.isSuccess)
                result.getOrNull()?.let { path ->
                    thumbPaths[messageTid.toString()] = ThumbPathsData(messageTid, path, size)
                    task.thumbCallback.onThumb(path)
                }
            preparingThumbsMap.remove(thumbKey)
        }
    }

    override fun clearPreparingThumbPaths() {
        preparingThumbsMap.clear()
    }

    private fun getPreparingThumbKey(messageTid: Long, size: Size) = "$messageTid$size"

    private fun checkAndUpload(attachment: SceytAttachment, task: TransferTask) {
        if (currentUploadingAttachment == null)
            uploadAttachment(attachment, task)
        else {
            if (currentUploadingAttachment?.filePath != attachment.filePath)
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
        checkAndResizeMessageAttachments(application, attachment) {
            if (it.isSuccess) {
                it.getOrNull()?.let { path ->
                    transferTask.updateFileLocationCallback.onUpdateFileLocation(path)
                }
            } else Log.i("resizeResult", "Couldn't resize file with reason ${it.exceptionOrNull()}")

            ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f) return
                    transferTask.progressCallback.onProgress(TransferData(transferTask.messageTid, attachment.tid,
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
        ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
            override fun onResult(progress: Float) {
                if (progress == 1f || pausedTasksMap[attachment.messageTid.toString()] != null) return
                getAppropriateTasks(transferTask).forEach { task ->
                    fileTransferService.getTasks()[task.messageTid.toString()]?.state = TransferState.Uploading
                    task.progressCallback.onProgress(TransferData(task.messageTid, task.attachment.tid,
                        progress * 100, TransferState.Uploading, task.attachment.filePath, null))
                }
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Error(exception))
                }
                sharedFilesPath.remove(attachment.filePath)
            }
        }, object : UrlCallback {
            override fun onResult(p0: String?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Success(p0))
                }
                sharedFilesPath.remove(attachment.filePath)
            }

            override fun onError(exception: SceytException?) {
                getAppropriateTasks(transferTask).forEach { task ->
                    task.resultCallback.onResult(SceytResponse.Error(exception))
                }
                sharedFilesPath.remove(attachment.filePath)
            }
        })
    }

    @Synchronized
    private fun getAppropriateTasks(transferTask: TransferTask): List<TransferTask> {
        return fileTransferService.getTasks().values.filter {
            it.attachment.filePath == transferTask.attachment.filePath
                    && it.state != TransferState.PauseUpload
        }
    }

    private fun checkAndResizeMessageAttachments(context: Context, attachment: SceytAttachment, callback: (Result<String?>) -> Unit) {
        when (attachment.type) {
            AttachmentTypeEnum.Image.value() -> {
                val result = resizeImage(context, attachment.filePath, 1080)
                callback(result)
            }
            AttachmentTypeEnum.Video.value() -> {
                transcodeVideo(context, attachment.filePath, callback)
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

    fun clear() {
        pausedTasksMap.clear()
        downloadingUrlMap.clear()
        sharedFilesPath.clear()
        preparingThumbsMap.clear()
    }

    data class ThumbPathsData(val messageTid: Long,
                              val path: String,
                              val size: Size)
}