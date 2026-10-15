package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.logger.SceytLog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OkHttpDownloader {

    private val downloadCalls = ConcurrentHashMap<String, Call>()

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
        onResult: (SceytResponse<String>) -> Unit,
    ): Call? {
        val operationId = request.operationId
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
            onResult(SceytResponse.Error(SceytException(0, "Invalid URL: ${e.message}")))
            return null
        }

        val call = httpClient.newCall(httpRequest)
        downloadCalls[operationId] = call

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) return
                downloadCalls.remove(operationId, call)
                SceytLog.e(TAG, "Download failed: ${e.message}")
                onResult(SceytResponse.Error(SceytException(0, e.message)))
            }

            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return

                try {
                    if (!response.isSuccessful) {
                        downloadCalls.remove(operationId, call)
                        onResult(
                            createErrorResponse(message = response.message, code = response.code)
                        )
                        return
                    }

                    val responseBody = response.body
                    val contentLength = responseBody.contentLength()
                    val totalSize = if (contentLength != -1L) contentLength + existingSize else -1L

                    val isPartialContent = response.code == 206
                    val outputStream = if (isPartialContent) {
                        java.io.FileOutputStream(destFile, true) // append mode
                    } else {
                        destFile.outputStream()
                    }

                    val sink = outputStream.sink().buffer()
                    val source = responseBody.source()

                    var downloadedBytes = existingSize
                    var lastProgressUpdate = 0L
                    val progressUpdateInterval = 100L // Update every 100ms

                    try {
                        while (!source.exhausted() && !call.isCanceled()) {
                            val bytesRead = source.read(sink.buffer, 8192)
                            if (bytesRead == -1L) break

                            sink.emit()
                            downloadedBytes += bytesRead

                            // Update progress with throttling
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressUpdate > progressUpdateInterval) {
                                if (totalSize > 0) {
                                    val progress =
                                        (downloadedBytes.toFloat() / totalSize.toFloat()) * 100
                                    onProgress(progress)
                                }
                                lastProgressUpdate = currentTime
                            }
                        }

                        sink.close()

                        if (call.isCanceled()) return

                        downloadCalls.remove(operationId, call)
                        onResult(SceytResponse.Success(destFile.absolutePath))
                    } catch (e: Exception) {
                        sink.close()
                        if (!call.isCanceled()) {
                            downloadCalls.remove(operationId, call)
                            SceytLog.e(TAG, "Download error: ${e.message}")
                            onResult(SceytResponse.Error(SceytException(0, e.message)))
                        }
                    }

                } catch (e: Exception) {
                    downloadCalls.remove(operationId, call)
                    SceytLog.e(TAG, "Download error: ${e.message}")
                    onResult(SceytResponse.Error(SceytException(0, e.message)))
                }
            }
        })
        return call
    }

    fun pauseDownload(operationId: String) {
        downloadCalls.remove(operationId)?.cancel()
    }

    fun cancelDownload(operationId: String, call: Call) {
        downloadCalls.remove(operationId, call)
        call.cancel()
    }

    fun resumeDownload(operationId: String): Boolean {
        // Check if there's already a download in progress
        return if (downloadCalls.containsKey(operationId)) {
            true // Already downloading
        } else {
            false // Can resume - the downloadFile method will handle partial download
        }
    }

    @Suppress("unused")
    fun cancelAllDownloads() {
        downloadCalls.values.forEach { it.cancel() }
        downloadCalls.clear()
    }

    @Suppress("unused")
    fun isDownloading(operationId: String): Boolean {
        return downloadCalls.containsKey(operationId)
    }

    @Suppress("unused")
    fun getActiveDownloadsCount(): Int {
        return downloadCalls.size
    }

    @Suppress("unused")
    fun getActiveDownloads(): List<String> {
        return downloadCalls.keys.toList()
    }

    @Suppress("unused")
    fun cancelDownload(operationId: String) {
        downloadCalls.remove(operationId)?.cancel()
    }
} 
