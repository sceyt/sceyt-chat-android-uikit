package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class FileTransferUtility {
    private val downloader = OkHttpDownloader()

    suspend fun uploadFile(
        attachment: SceytAttachment,
        onProgress: (Float) -> Unit,
    ): SceytResponse<String> = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()

        fun complete(response: SceytResponse<String>) {
            if (!continuation.isActive || !completed.compareAndSet(false, true)) return

            continuation.resume(response)
        }

        val operation = ChatClient.getClient().uploadCancellable(
            /* filePath = */ attachment.filePath,
            /* progressCallback = */ object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f || !continuation.isActive) return
                    onProgress(progress * 100)
                }

                override fun onError(exception: SceytException?) {
                    SceytLog.e(TAG, "Error upload file ${exception?.message}")
                    complete(SceytResponse.Error(exception))
                }
            },
            /* actionCallback = */ object : UrlCallback {
                override fun onResult(url: String?) {
                    complete(SceytResponse.Success(url))
                }

                override fun onError(exception: SceytException?) {
                    SceytLog.e(TAG, "Error upload file ${exception?.message}")
                    complete(SceytResponse.Error(exception))
                }
            }
        )

        continuation.invokeOnCancellation {
            completed.set(true)
            operation.cancel()
        }
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
