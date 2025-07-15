package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class FileTransferUtility {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uploadJobs = hashMapOf<Long, Job>()
    private val downloader = OkHttpDownloader()

    fun uploadFile(
            attachment: SceytAttachment,
            onProgress: (Float) -> Unit,
            onResult: (SceytResponse<String>) -> Unit,
    ) {
        val job = scope.launch {
            callbackFlow<Any> {
                ChatClient.getClient().upload(attachment.filePath, object : ProgressCallback {
                    override fun onResult(progress: Float) {
                        if (progress == 1f || !this@launch.isActive) return
                        onProgress(progress * 100)
                    }

                    override fun onError(exception: SceytException?) {
                        SceytLog.e(TAG, "Error upload file ${exception?.message}")
                        if (!isActive) return
                        onResult(SceytResponse.Error(exception))
                    }
                }, object : UrlCallback {
                    override fun onResult(p0: String?) {
                        if (!isActive) return
                        onResult(SceytResponse.Success(p0))
                        channel.close()
                    }

                    override fun onError(exception: SceytException?) {
                        SceytLog.e(TAG, "Error upload file ${exception?.message}")
                        if (!isActive) return
                        onResult(SceytResponse.Error(exception))
                        channel.close()
                    }
                })

                awaitClose {
                    uploadJobs.remove(attachment.messageTid)
                }
            }.launchIn(this)
        }
        uploadJobs[attachment.messageTid] = job
    }

    fun pauseUpload(attachment: SceytAttachment) {
        uploadJobs[attachment.messageTid]?.cancel()
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