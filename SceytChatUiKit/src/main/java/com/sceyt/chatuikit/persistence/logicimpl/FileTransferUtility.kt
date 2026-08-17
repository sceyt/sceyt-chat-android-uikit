package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class FileTransferUtility {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Conditional removal keeps an older job from deleting its same-message replacement.
    private val uploadJobs = ConcurrentHashMap<Long, Job>()
    private val downloader = OkHttpDownloader()

    fun uploadFile(
        attachment: SceytAttachment,
        onProgress: (Float) -> Unit,
        onResult: (SceytResponse<String>) -> Unit,
    ) {
        val messageTid = attachment.messageTid
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                awaitUpload(attachment, onProgress, onResult)
            } finally {
                uploadJobs.remove(messageTid, currentCoroutineContext().job)
            }
        }
        uploadJobs.put(messageTid, job)?.cancel()
        job.start()
    }

    fun pauseUpload(attachment: SceytAttachment) {
        uploadJobs.remove(attachment.messageTid)?.cancel()
    }

    private suspend fun awaitUpload(
        attachment: SceytAttachment,
        onProgress: (Float) -> Unit,
        onResult: (SceytResponse<String>) -> Unit,
    ) = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        continuation.invokeOnCancellation { completed.set(true) }

        fun complete(response: SceytResponse<String>) {
            if (!continuation.isActive || !completed.compareAndSet(false, true)) return

            try {
                onResult(response)
            } finally {
                continuation.resume(Unit)
            }
        }

        ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
            override fun onResult(progress: Float) {
                if (progress == 1f || !continuation.isActive) return
                onProgress(progress * 100)
            }

            override fun onError(exception: SceytException?) {
                SceytLog.e(TAG, "Error upload file ${exception?.message}")
                complete(SceytResponse.Error(exception))
            }
        }, object : UrlCallback {
            override fun onResult(url: String?) {
                complete(SceytResponse.Success(url))
            }

            override fun onError(exception: SceytException?) {
                SceytLog.e(TAG, "Error upload file ${exception?.message}")
                complete(SceytResponse.Error(exception))
            }
        })
    }

    fun resumeUpload(attachment: SceytAttachment): Boolean {
        // not implemented
        return false
    }

    fun downloadFile(
        attachment: SceytAttachment,
        destFile: File,
        onProgress: (Float) -> Unit,
        onResult: (SceytResponse<String>) -> Unit,
    ) {
        downloader.downloadFile(attachment, destFile, onProgress, onResult)
    }

    fun pauseDownload(attachment: SceytAttachment) {
        downloader.pauseDownload(attachment)
    }

    fun resumeDownload(attachment: SceytAttachment): Boolean {
        return downloader.resumeDownload(attachment)
    }
}
