package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class FileTransferServiceImpl : FileTransferService, CoroutineScope {
    var listeners: FileTransferListeners.Listeners = object : FileTransferListenersImpl() {

        override fun upload(messageTid: Long, attachmentTid: Long, path: String, type: AttachmentTypeEnum, progressCallback: ProgressUpdateCallback, resultCallback: UploadResult) {

            uploadFile(messageTid, attachmentTid, path, type, resultCallback, progressCallback)
        }
    }


    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        listeners = fileTransferListeners
    }

    override fun upload(messageTid: Long,
                        attachmentTid: Long,
                        path: String,
                        type: AttachmentTypeEnum,
                        progressCallback: ProgressUpdateCallback,
                        resultCallback: UploadResult) {

        listeners.upload(messageTid, attachmentTid, path, type, progressCallback, resultCallback)
    }

    override fun download(tid: Long, path: String) {
        listeners.download(tid, path)
    }

    private fun uploadFile(messageTid: Long, tid: Long, path: String, type: AttachmentTypeEnum, resultCallback: UploadResult, progressCallback: ProgressUpdateCallback) {
        progressCallback.onProgress(TransferData(messageTid, tid, 0.02f, ProgressState.Pending, path))
        ChatClient.getClient().upload(path, object : ProgressCallback {
            override fun onResult(progress: Float) {
                if (progress==1f) return
                Log.i("sdfsdsfdf", "push$progress")
                progressCallback.onProgress(TransferData(messageTid, tid, progress, ProgressState.Uploading, path))
            }

            override fun onError(exception: SceytException?) {
                resultCallback.onResult(SceytResponse.Error(exception))
            }
        }, object : UrlCallback {
            override fun onResult(p0: String?) {
                Log.i("sdfsdsfdf", "push finish $p0")
                resultCallback.onResult(SceytResponse.Success(p0))
            }

            override fun onError(exception: SceytException?) {
                resultCallback.onResult(SceytResponse.Error(exception))
            }
        })
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}

fun interface UploadResult {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

data class TransferData(
        val messageTid: Long,
        val attachmentTid: Long,
        val progressPercent: Float,
        val state: ProgressState,
        val path: String?
)

enum class ProgressState {
    Pending,
    Uploading,
    Uploaded,
    Downloading,
    Downloaded,
    Error
}
