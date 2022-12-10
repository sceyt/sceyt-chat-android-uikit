package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.app.Application
import android.util.Log
import com.koushikdutta.ion.Ion
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.presentation.common.getLocaleFileByNameOrMetadata
import java.io.File

class FileTransferServiceImpl(private var application: Application) : FileTransferService {

    private var listeners: FileTransferListeners.Listeners = object : FileTransferListeners.Listeners {
        override fun upload(messageTid: Long, attachment: Attachment,
                            progressCallback: ProgressUpdateCallback, resultCallback: TransferResult) {
            uploadFile(messageTid, attachment, resultCallback, progressCallback)
        }

        override fun download(messageTid: Long, attachment: Attachment,
                              progressCallback: ProgressUpdateCallback, resultCallback: TransferResult) {
            downloadFile(messageTid, attachment, progressCallback, resultCallback)
        }
    }

    override fun upload(messageTid: Long, attachment: Attachment,
                        progressCallback: ProgressUpdateCallback,
                        resultCallback: TransferResult) {
        listeners.upload(messageTid, attachment, progressCallback, resultCallback)
    }

    override fun download(messageTid: Long, attachment: Attachment, progressCallback: ProgressUpdateCallback,
                          resultCallback: TransferResult) {
        listeners.download(messageTid, attachment, progressCallback, resultCallback)
    }

    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        listeners = fileTransferListeners
    }

    // Default logic
    private fun uploadFile(messageTid: Long, attachment: Attachment, resultCallback: TransferResult, progressCallback: ProgressUpdateCallback) {
        ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
            override fun onResult(progress: Float) {
                progressCallback.onProgress(TransferData(messageTid, attachment.tid, progress * 100, ProgressState.Uploading, attachment.filePath, ""))
            }

            override fun onError(exception: SceytException?) {
                resultCallback.onResult(SceytResponse.Error(exception))
            }
        }, object : UrlCallback {
            override fun onResult(p0: String?) {
                resultCallback.onResult(SceytResponse.Success(p0))
            }

            override fun onError(exception: SceytException?) {
                resultCallback.onResult(SceytResponse.Error(exception))
            }
        })
    }

    private fun downloadFile(messageTid: Long, attachment: Attachment, progressCallback: ProgressUpdateCallback, resultCallback: TransferResult) {
        val loadedFile = File(application.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            resultCallback.onResult(SceytResponse.Success(file.path))
        } else {
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()

            Ion.with(application)
                .load(attachment.url)
                .progress { downloaded, total ->
                    val progress = ((downloaded / total.toFloat())) * 100f
                    Log.i("sdfsf", progress.toString())

                    progressCallback.onProgress(TransferData(
                        messageTid, attachment.tid, progress, ProgressState.Downloading, null, attachment.url))
                }
                .write(loadedFile)
                .setCallback { e, result ->
                    if (result == null && e != null) {
                        loadedFile.delete()
                        resultCallback.onResult(SceytResponse.Error(SceytException(0, e.message)))
                    } else
                        resultCallback.onResult(SceytResponse.Success(result.path))
                }
        }
    }
}

fun interface TransferResult {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

