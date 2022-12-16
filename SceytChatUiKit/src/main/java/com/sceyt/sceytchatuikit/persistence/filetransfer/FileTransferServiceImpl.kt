package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.app.Application
import com.koushikdutta.ion.Ion
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.presentation.common.getLocaleFileByNameOrMetadata
import java.io.File

class FileTransferServiceImpl(private var application: Application) : FileTransferService {
    private var tasksMap = hashMapOf<String, TransferTask>()
    private var downloadingUrlMap = hashMapOf<String, String>()

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

    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        listeners = fileTransferListeners
    }

    // Default logic
    private fun uploadFile(attachment: SceytAttachment, payLoad: TransferTask) {
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

    private fun downloadFile(attachment: SceytAttachment, payLoad: TransferTask) {
        val loadedFile = File(application.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            payLoad.resultCallback.onResult(SceytResponse.Success(file.path))
        } else {
            if (downloadingUrlMap[attachment.url] != null) return
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            payLoad.progressCallback.onProgress(TransferData(
                payLoad.messageTid, attachment.tid, 0f, Downloading, null, attachment.url))
            attachment.url?.let { url ->
                downloadingUrlMap[url] = url
                Ion.with(application)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        val progress = ((downloaded / total.toFloat())) * 100
                        payLoad.progressCallback.onProgress(TransferData(
                            payLoad.messageTid, attachment.tid, progress, Downloading, null, attachment.url))
                    }
                    .write(loadedFile)
                    .setCallback { e, result ->
                        if (result == null && e != null) {
                            loadedFile.delete()
                            payLoad.resultCallback.onResult(SceytResponse.Error(SceytException(0, e.message)))
                            downloadingUrlMap.remove(attachment.url)
                        } else
                            payLoad.resultCallback.onResult(SceytResponse.Success(result.path))
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
            PendingDownload -> {
                tasksMap[attachment.url.toString()]?.let {
                    downloadingUrlMap.remove(attachment.url)
                    downloadFile(attachment, it)
                }
            }
            PendingUpload -> {
                tasksMap[attachment.tid.toString()]?.let {
                    uploadFile(attachment, it)
                }
            }
            else -> {}
        }
    }
}

fun interface TransferResultCallback {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

