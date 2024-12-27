package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.koushikdutta.ion.Ion
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

class FileTransferUtility(
        private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uploadJobs = hashMapOf<Long, Job>()

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
        Ion.with(context)
            .load(attachment.url)
            .progress { downloaded, total ->
                val progress = ((downloaded / total.toFloat())) * 100
                onProgress(progress)
            }
            .group(attachment.url)
            .write(destFile)
            .setCallback { e, result ->
                if (result == null && e != null) {
                    onResult(SceytResponse.Error(SceytException(0, e.message)))
                } else
                    onResult(SceytResponse.Success(result.path))
            }
    }


    fun pauseDownload(attachment: SceytAttachment) {
        Ion.getDefault(context).cancelAll(attachment.url)
    }

    fun resumeDownload(attachment: SceytAttachment): Boolean {
        // not implemented
        return false
    }
}