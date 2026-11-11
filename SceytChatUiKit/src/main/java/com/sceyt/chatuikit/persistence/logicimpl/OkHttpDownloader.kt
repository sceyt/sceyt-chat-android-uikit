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
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Downloads HTTP content into a file and resumes partial files using byte ranges. */
class OkHttpDownloader {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val original = chain.call().request()
                val redirected = chain.request()
                val sameOrigin = original.url.scheme == redirected.url.scheme &&
                        original.url.host == redirected.url.host &&
                        original.url.port == redirected.url.port

                // Caller headers can contain credentials, so keep them on the original origin.
                val nextRequest = if (sameOrigin) redirected else redirected.newBuilder().apply {
                    original.headers.names().forEach { removeHeader(it) }
                }.build()
                chain.proceed(nextRequest)
            }
            .build()
    }

    fun downloadFile(
        request: FileDownloadRequest,
        onProgress: (Float) -> Unit,
        onResult: (FileTransferResult<String>) -> Unit,
        onFinished: () -> Unit = {},
    ): Call? {
        val destinationFile = request.destinationFile
        destinationFile.parentFile?.mkdirs()

        val existingSize = if (destinationFile.exists()) destinationFile.length() else 0L
        val httpRequest = try {
            Request.Builder()
                .url(request.url)
                .apply {
                    request.headers.forEach { (name, value) -> header(name, value) }
                    if (existingSize > 0) header("Range", "bytes=$existingSize-")
                }
                .build()
        } catch (error: Exception) {
            SceytLog.e(TAG, "Invalid URL: ${error.message}")
            try {
                val exception = SceytException(0, "Invalid URL: ${error.message}")
                onResult(FileTransferResult.Failure(exception))
            } finally {
                onFinished()
            }

            // Request creation failed before an HTTP call could be started.
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
                    // An explicitly canceled call only needs its cleanup completion signal.
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

                        // Cancellation can finish a queued call before its response arrives.
                        if (!canReadResponse) return
                        if (call.isCanceled()) return

                        if (!response.isSuccessful) {
                            val error = SceytException(response.code, response.message)
                            val result = when (response.code) {
                                408, 429, 500, 502, 503, 504 -> FileTransferResult.Interrupted(error)
                                else -> FileTransferResult.Failure(error)
                            }
                            onResult(result)
                            // Handle HTTP errors before opening or changing the destination.
                            return
                        }

                        val responseBody = response.body
                        val contentLength = responseBody.contentLength()

                        // Append for a range response. A full response starts the file again.
                        val isPartialResponse = response.code == 206
                        val resumedBytes = if (isPartialResponse) existingSize else 0L
                        val totalSize =
                            if (contentLength != -1L) contentLength + resumedBytes else -1L
                        val outputStream = FileOutputStream(destinationFile, isPartialResponse)

                        outputStream.sink().buffer().use { sink ->
                            val source = responseBody.source()
                            var downloadedBytes = resumedBytes
                            var lastProgressUpdate = 0L
                            val progressUpdateInterval = 100L

                            while (!call.isCanceled()) {
                                val bytesRead = try {
                                    source.read(sink.buffer, 8192)
                                } catch (error: IOException) {
                                    if (!call.isCanceled())
                                        onResult(FileTransferResult.Interrupted(error))

                                    // Keep written bytes so a later attempt can request the remaining range.
                                    return
                                }
                                if (bytesRead == -1L) break

                                // Destination writes fail terminally; only network reads are resumable.
                                sink.emit()
                                downloadedBytes += bytesRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastProgressUpdate > progressUpdateInterval) {
                                    if (totalSize > 0)
                                        onProgress(downloadedBytes.toFloat() / totalSize.toFloat() * 100)
                                    lastProgressUpdate = currentTime
                                }
                            }
                        }
                        if (!call.isCanceled()) onResult(FileTransferResult.Success(destinationFile.absolutePath))
                    }
                } catch (error: Exception) {
                    if (!call.isCanceled()) {
                        SceytLog.e(TAG, "Download error: ${error.message}")
                        onResult(FileTransferResult.Failure(error))
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
