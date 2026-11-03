package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.ChatClient
import com.sceyt.chat.UploadOperation
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.EncryptedUploadCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class FileTransferUtility {
    private val downloader = OkHttpDownloader()

    suspend fun uploadEncryptedFile(
        source: PreparedUpload,
        onProgress: (Float) -> Unit,
    ): SceytResponse<String> = awaitSdkUpload(onProgress) { progress, result ->
        ChatClient.getClient().uploadEncrypted(
            source,
            progress,
            object : EncryptedUploadCallback {
                override fun onResult(url: String?, encryptionInfo: FileEncryptionInfo?) {
                    result.onResult(url)
                }

                override fun onError(exception: SceytException?) {
                    result.onError(exception)
                }
            },
        )
    }

    suspend fun uploadFile(
        sourceFile: File,
        onProgress: (Float) -> Unit,
    ): SceytResponse<String> = awaitSdkUpload(onProgress) { progress, result ->
        ChatClient.getClient().uploadCancellable(sourceFile.path, progress, result)
    }

    private suspend fun awaitSdkUpload(
        onProgress: (Float) -> Unit,
        startUpload: (ProgressCallback, UrlCallback) -> UploadOperation,
    ): SceytResponse<String> = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()

        fun complete(response: SceytResponse<String>) {
            if (!continuation.isActive || !completed.compareAndSet(false, true)) return
            if (response is SceytResponse.Error) {
                SceytLog.e(TAG, "Error upload file ${response.exception?.message}")
            }
            continuation.resume(response)
        }

        val operation = startUpload(
            object : ProgressCallback {
                override fun onResult(progress: Float) {
                    if (progress == 1f || completed.get() || !continuation.isActive) return
                    onProgress(progress * 100)
                }

                override fun onError(exception: SceytException?) {
                    complete(SceytResponse.Error(exception))
                }
            },
            object : UrlCallback {
                override fun onResult(url: String?) {
                    complete(SceytResponse.Success(url))
                }

                override fun onError(exception: SceytException?) {
                    complete(SceytResponse.Error(exception))
                }
            },
        )

        continuation.invokeOnCancellation {
            completed.set(true)
            operation.cancel()
        }
    }

    fun downloadFile(
        request: FileDownloadRequest,
        onProgress: (Float) -> Unit,
        onResult: (FileTransferResult<String>) -> Unit,
        onFinished: () -> Unit = {},
    ): Call? {
        return downloader.downloadFile(request, onProgress, onResult, onFinished)
    }

    fun cancelDownload(call: Call) {
        downloader.cancelDownload(call)
    }
}
