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
import com.sceyt.sceytchatuikit.presentation.common.getLocaleFileByNameOrMetadata
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.math.min

internal class FileTransferLogicImpl(private val application: Application) : FileTransferLogic, SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, String>()
    private var preparingThumbsMap = hashMapOf<Long, Long>()
    private var pendingUploadQue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private var uploading: Boolean = false

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        checkAndUpload(attachment, task)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        val loadedFile = File(application.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

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
                        val progress = ((downloaded / total.toFloat())) * 100
                        task.progressCallback.onProgress(TransferData(
                            task.messageTid, attachment.tid, progress, TransferState.Downloading, null, attachment.url))
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
        when (state) {
            TransferState.PendingUpload, TransferState.Uploading -> {
                //todo
                //uploadNext()
            }
            TransferState.PendingDownload, TransferState.Downloading -> {
                //todo
            }
            else -> {}
        }
    }

    override fun resumeLoad(attachment: SceytAttachment) {
        when (attachment.transferState) {
            TransferState.PendingDownload, TransferState.PauseDownload, TransferState.ErrorDownload -> {
                fileTransferService.getTasks()[attachment.url.toString()]?.let {
                    downloadingUrlMap.remove(attachment.url)
                    downloadFile(attachment, it)
                }
            }
            TransferState.PendingUpload, TransferState.PauseUpload, TransferState.ErrorUpload -> {
                fileTransferService.getTasks()[attachment.tid.toString()]?.let {
                    uploadFile(attachment, it)
                }
            }
            else -> {}
        }
    }

    override fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, size: Size) {
        if (preparingThumbsMap[messageTid] != null) return
        preparingThumbsMap[messageTid] = messageTid
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        thumbPaths[messageTid.toString()]?.let {
            task.thumbCallback.onThumb(it)
            preparingThumbsMap.remove(messageTid)
            return
        } ?: run {
            val result = getAttachmentThumbPath(application, attachment, size)
            if (result.isSuccess)
                result.getOrNull()?.let { path ->
                    thumbPaths[messageTid.toString()] = path
                    task.thumbCallback.onThumb(path)
                }
            preparingThumbsMap.remove(messageTid)
        }
    }

    private fun checkAndUpload(attachment: SceytAttachment, task: TransferTask) {
        if (uploading.not())
            uploadAttachment(attachment, task)
        else pendingUploadQue.add(Pair(attachment, task))
    }

    private fun uploadNext() {
        uploading = false
        if (pendingUploadQue.isEmpty()) return
        pendingUploadQue.poll()?.let {
            uploadAttachment(it.first, it.second)
        }
    }

    private fun uploadAttachment(attachment: SceytAttachment, transferTask: TransferTask) {
        uploading = true
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

    private fun checkAndResizeMessageAttachments(context: Context, attachment: SceytAttachment, callback: (Result<String?>) -> Unit) {
        when (attachment.type) {
            AttachmentTypeEnum.Image.value() -> {
                val result = resizeImage(context, attachment.filePath)
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
        val minSize = min(size.height, size.width)
        val reqSize = if (minSize > 0) minSize.toFloat() else 500f
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
}