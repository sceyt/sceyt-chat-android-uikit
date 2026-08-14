package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OkHttpDownloader {

    // Keyed by the downloading file, not by the message, because a message can download
    // more than one file, like a video with its thumb
    private val downloadCalls = DownloadCallRegistry()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun downloadFile(
        attachment: SceytAttachment,
        destFile: File,
        onProgress: (Float) -> Unit,
        onResult: (SceytResponse<String>) -> Unit,
    ) {
        val url = attachment.url ?: run {
            onResult(SceytResponse.Error(SceytException(0, "URL is null")))
            return
        }
        val downloadKey = DownloadKey(attachment.messageTid, url)

        // Create parent directories if they don't exist
        destFile.parentFile?.mkdirs()

        val existingSize = if (destFile.exists()) destFile.length() else 0L
        val request = try {
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
            return
        }

        val call = httpClient.newCall(request)
        downloadCalls.track(downloadKey, call)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) return
                downloadCalls.remove(downloadKey, call)
                SceytLog.e(TAG, "Download failed: ${e.message}")
                onResult(SceytResponse.Error(SceytException(0, e.message)))
            }

            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return

                try {
                    if (!response.isSuccessful) {
                        downloadCalls.remove(downloadKey, call)
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

                        downloadCalls.remove(downloadKey, call)
                        onResult(SceytResponse.Success(destFile.absolutePath))
                    } catch (e: Exception) {
                        sink.close()
                        if (!call.isCanceled()) {
                            downloadCalls.remove(downloadKey, call)
                            SceytLog.e(TAG, "Download error: ${e.message}")
                            onResult(SceytResponse.Error(SceytException(0, e.message)))
                        }
                    }

                } catch (e: Exception) {
                    downloadCalls.remove(downloadKey, call)
                    SceytLog.e(TAG, "Download error: ${e.message}")
                    onResult(SceytResponse.Error(SceytException(0, e.message)))
                }
            }
        })
    }

    fun pauseDownload(attachment: SceytAttachment) {
        attachment.downloadKey?.let(downloadCalls::cancel)
    }

    fun resumeDownload(attachment: SceytAttachment): Boolean {
        // Check if there's already a download in progress
        return if (attachment.downloadKey?.let(downloadCalls::contains) == true) {
            true // Already downloading
        } else {
            false // Can resume - the downloadFile method will handle partial download
        }
    }

    @Suppress("unused")
    fun cancelAllDownloads() {
        downloadCalls.cancelAll()
    }

    @Suppress("unused")
    fun isDownloading(messageTid: Long): Boolean {
        return downloadCalls.containsMessage(messageTid)
    }

    @Suppress("unused")
    fun getActiveDownloadsCount(): Int {
        return downloadCalls.size
    }

    @Suppress("unused")
    fun getActiveDownloads(): List<Long> {
        return downloadCalls.messageTids()
    }

    @Suppress("unused")
    fun cancelDownload(messageTid: Long) {
        downloadCalls.cancelMessage(messageTid)
    }

    private val SceytAttachment.downloadKey: DownloadKey?
        get() = url?.let { DownloadKey(messageTid, it) }
}

internal data class DownloadKey(
    val messageTid: Long,
    val url: String,
)

internal class DownloadCallRegistry {
    private val calls = ConcurrentHashMap<DownloadKey, Call>()

    val size: Int
        get() = calls.size

    fun track(key: DownloadKey, call: Call) {
        calls.put(key, call)?.cancel()
    }

    fun remove(key: DownloadKey, call: Call): Boolean {
        return calls.remove(key, call)
    }

    fun contains(key: DownloadKey): Boolean {
        return calls.containsKey(key)
    }

    fun containsMessage(messageTid: Long): Boolean {
        return calls.keys.any { it.messageTid == messageTid }
    }

    fun messageTids(): List<Long> {
        return calls.keys.map(DownloadKey::messageTid).distinct()
    }

    fun cancel(key: DownloadKey) {
        calls.remove(key)?.cancel()
    }

    fun cancelMessage(messageTid: Long) {
        calls.keys.filter { it.messageTid == messageTid }.forEach { key ->
            calls.remove(key)?.cancel()
        }
    }

    fun cancelAll() {
        calls.values.forEach(Call::cancel)
        calls.clear()
    }
}
