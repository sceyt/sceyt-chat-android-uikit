package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.logger.SceytLog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class OkHttpDownloader {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun downloadFile(
        request: FileDownloadRequest,
        onProgress: (Float) -> Unit,
        onResult: (FileTransferResult<String>) -> Unit,
        onFinished: () -> Unit = {},
    ): Call? {
        val url = request.url
        val destFile = request.destinationFile

        // Create parent directories if they don't exist
        destFile.parentFile?.mkdirs()

        val existingSize = if (destFile.exists()) destFile.length() else 0L
        val httpRequest = try {
            Request.Builder()
                .url(url)
                .apply {
                    if (existingSize > 0) {
                        addHeader("Range", "bytes=$existingSize-")
                    }
                }
                .build()
        } catch (e: Exception) {
            SceytLog.e(TAG, "Invalid URL: ${e.message}")
            try {
                onResult(FileTransferResult.Failure(SceytException(0, "Invalid URL: ${e.message}")))
            } finally {
                onFinished()
            }
            return null
        }

        val call = httpClient.newCall(httpRequest)
        val state = AtomicReference(DownloadState.Pending)

        fun finish() {
            val previousState = state.getAndSet(DownloadState.Finished)
            if (previousState != DownloadState.Finished) onFinished()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                try {
                    if (call.isCanceled()) return
                    SceytLog.e(TAG, "Download failed: ${e.message}")
                    onResult(FileTransferResult.Interrupted(e))
                } finally {
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        val canReadResponse =
                            state.compareAndSet(DownloadState.Pending, DownloadState.Reading)
                        if (!canReadResponse || call.isCanceled()) return
                        if (!response.isSuccessful) {
                            val error = SceytException(response.code, response.message)
                            onResult(FileTransferResult.Failure(error))
                            return
                        }

                        val responseBody = response.body
                        val contentLength = responseBody.contentLength()
                        val resumedBytes = if (response.code == 206) existingSize else 0L
                        val totalSize =
                            if (contentLength != -1L) contentLength + resumedBytes else -1L
                        val outputStream = if (response.code == 206) {
                            java.io.FileOutputStream(destFile, true)
                        } else destFile.outputStream()

                        outputStream.sink().buffer().use { sink ->
                            val source = responseBody.source()
                            var downloadedBytes = resumedBytes
                            var lastProgressUpdate = 0L
                            val progressUpdateInterval = 100L

                            while (!call.isCanceled()) {
                                val bytesRead = try {
                                    source.read(sink.buffer, 8192)
                                } catch (e: IOException) {
                                    if (!call.isCanceled())
                                        onResult(FileTransferResult.Interrupted(e))
                                    return
                                }
                                if (bytesRead == -1L) break
                                // Destination writes fail terminally; only network reads are resumable.
                                sink.emit()
                                downloadedBytes += bytesRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastProgressUpdate > progressUpdateInterval) {
                                    if (totalSize > 0) {
                                        onProgress(downloadedBytes.toFloat() / totalSize.toFloat() * 100)
                                    }
                                    lastProgressUpdate = currentTime
                                }
                            }
                        }
                        if (!call.isCanceled()) onResult(FileTransferResult.Success(destFile.absolutePath))
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        SceytLog.e(TAG, "Download error: ${e.message}")
                        onResult(FileTransferResult.Failure(e))
                    }
                } finally {
                    // Signal only after both the file sink and response have closed.
                    finish()
                }
            }
        })
        return object : Call by call {
            override fun cancel() {
                call.cancel()
                // No writer owns the destination yet; a late response cannot start one.
                val cancelledBeforeReading =
                    state.compareAndSet(DownloadState.Pending, DownloadState.Finished)
                if (cancelledBeforeReading) onFinished()
            }
        }
    }

    fun cancelDownload(call: Call) {
        call.cancel()
    }

    private enum class DownloadState { Pending, Reading, Finished }
}
