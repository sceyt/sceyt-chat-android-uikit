package com.sceyt.sceytchatuikit.persistence.filetransfer

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
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.persistence.extensions.transcodeVideo
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.presentation.common.getLocaleFileByNameOrMetadata
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import java.io.File
import java.io.FileNotFoundException
import kotlin.collections.set
import kotlin.math.min

class FileTransferServiceImpl(private var application: Application) : FileTransferService {
    private var tasksMap = hashMapOf<String, TransferTask>()
    private var downloadingUrlMap = hashMapOf<String, String>()
    private var thumbPaths = hashMapOf<String, String>()
    private var preparingThumbsMap = hashMapOf<Long, Long>()

    private var listeners: FileTransferListeners.Listeners = object : FileTransferListeners.Listeners {
        override fun upload(attachment: SceytAttachment,
                            transferTask: TransferTask) {
            uploadFile(attachment, transferTask)
        }

        override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
            downloadFile(attachment, transferTask)
        }

        override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
            pauseLoad(attachment, state)
        }

        override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
            resumeLoad(attachment)
        }

        override fun getThumb(messageTid: Long, attachment: SceytAttachment, size: Size) {
            getAttachmentThumb(messageTid, attachment, size)
        }
    }


    override fun upload(attachment: SceytAttachment,
                        transferTask: TransferTask) {
        tasksMap[attachment.tid.toString()] = transferTask
        listeners.upload(attachment, transferTask)
    }

    override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
        tasksMap[attachment.url.toString()] = transferTask
        listeners.download(attachment, transferTask)
    }

    override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        listeners.pause(messageTid, attachment, state)
    }

    override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        listeners.resume(messageTid, attachment, state)
    }

    override fun getThumb(messageTid: Long, attachment: SceytAttachment, size: Size) {
        listeners.getThumb(messageTid, attachment, size)
    }

    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        listeners = fileTransferListeners
    }

    override fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask {
        val isUploading: Boolean
        val key: String
        when (attachment.transferState) {
            PendingDownload, Downloading, Downloaded, PauseDownload -> {
                isUploading = false
                key = attachment.url.toString()
            }
            else -> {
                isUploading = true
                key = attachment.tid.toString()
            }
        }
        return tasksMap[key] ?: run {
            FileTransferHelper.createTransferTask(attachment, isUploading)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        val key: String = when (attachment.transferState) {
            PendingDownload, Downloading, Downloaded, PauseDownload -> {
                attachment.url.toString()
            }
            else -> attachment.tid.toString()
        }
        return tasksMap[key]
    }

    // Default logic
    private fun uploadFile(attachment: SceytAttachment, payLoad: TransferTask) {
        checkAndResizeMessageAttachments(application, attachment) {
            if (it.isSuccess) {
                it.getOrNull()?.let { path ->
                    payLoad.updateFileLocationCallback.onUpdateFileLocation(path)
                }
            } else Log.i("resizeResult", "Couldn't resize file with reason ${it.exceptionOrNull()}")

            ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f) return
                    payLoad.progressCallback.onProgress(TransferData(payLoad.messageTid, attachment.tid,
                        progress * 100, Uploading, attachment.filePath, null))
                }

                override fun onError(exception: SceytException?) {
                    payLoad.resultCallback.onResult(SceytResponse.Error(exception))
                }
            }, object : UrlCallback {
                override fun onResult(p0: String?) {
                    payLoad.resultCallback.onResult(SceytResponse.Success(p0))
                }

                override fun onError(exception: SceytException?) {
                    payLoad.resultCallback.onResult(SceytResponse.Error(exception))
                }
            })
        }
    }

    private fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        val loadedFile = File(application.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            task.resultCallback.onResult(SceytResponse.Success(file.path))
        } else {
            if (downloadingUrlMap[attachment.url] != null) return
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            task.progressCallback.onProgress(TransferData(
                task.messageTid, attachment.tid, 0f, Downloading, null, attachment.url))
            attachment.url?.let { url ->
                downloadingUrlMap[url] = url
                Ion.with(application)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        val progress = ((downloaded / total.toFloat())) * 100
                        task.progressCallback.onProgress(TransferData(
                            task.messageTid, attachment.tid, progress, Downloading, null, attachment.url))
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

    private fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, Uploading -> {
                //todo
            }
            PendingDownload, Downloading -> {
                //todo
            }
            else -> {}
        }
    }

    private fun resumeLoad(attachment: SceytAttachment) {
        when (attachment.transferState) {
            PendingDownload, PauseDownload, ErrorDownload -> {
                tasksMap[attachment.url.toString()]?.let {
                    downloadingUrlMap.remove(attachment.url)
                    downloadFile(attachment, it)
                }
            }
            PendingUpload, PauseUpload, ErrorUpload -> {
                tasksMap[attachment.tid.toString()]?.let {
                    uploadFile(attachment, it)
                }
            }
            else -> {}
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

    private fun getAttachmentThumb(messageTid: Long, attachment: SceytAttachment, size: Size) {
        if (preparingThumbsMap[messageTid] != null) return
        preparingThumbsMap[messageTid] = messageTid
        val task = findOrCreateTransferTask(attachment)
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
}

fun interface TransferResultCallback {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

fun interface UpdateFileLocationCallback {
    fun onUpdateFileLocation(path: String)
}

fun interface ThumbCallback {
    fun onThumb(path: String)
}

