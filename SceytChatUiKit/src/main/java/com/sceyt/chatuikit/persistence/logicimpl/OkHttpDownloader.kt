package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
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
    
    private val downloadCalls = ConcurrentHashMap<Long, Call>()
    
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
        val messageTid = attachment.messageTid
        val url = attachment.url ?: run {
            onResult(SceytResponse.Error(SceytException(0, "URL is null")))
            return
        }

        // Create parent directories if they don't exist
        destFile.parentFile?.mkdirs()

        val existingSize = if (destFile.exists()) destFile.length() else 0L
        val request = Request.Builder()
            .url(url)
            .apply {
                if (existingSize > 0) {
                    addHeader("Range", "bytes=$existingSize-")
                }
            }
            .build()

        val call = httpClient.newCall(request)
        downloadCalls[messageTid] = call

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) return
                downloadCalls.remove(messageTid)
                SceytLog.e(TAG, "Download failed: ${e.message}")
                onResult(SceytResponse.Error(SceytException(0, e.message)))
            }

            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return
                
                try {
                    if (!response.isSuccessful) {
                        downloadCalls.remove(messageTid)
                        onResult(SceytResponse.Error(SceytException(response.code, response.message)))
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
                                    val progress = (downloadedBytes.toFloat() / totalSize.toFloat()) * 100
                                    onProgress(progress)
                                }
                                lastProgressUpdate = currentTime
                            }
                        }
                        
                        sink.close()
                        
                        if (call.isCanceled()) return
                        
                        downloadCalls.remove(messageTid)
                        onResult(SceytResponse.Success(destFile.absolutePath))
                    } catch (e: Exception) {
                        sink.close()
                        if (!call.isCanceled()) {
                            downloadCalls.remove(messageTid)
                            SceytLog.e(TAG, "Download error: ${e.message}")
                            onResult(SceytResponse.Error(SceytException(0, e.message)))
                        }
                    }
                    
                } catch (e: Exception) {
                    downloadCalls.remove(messageTid)
                    SceytLog.e(TAG, "Download error: ${e.message}")
                    onResult(SceytResponse.Error(SceytException(0, e.message)))
                }
            }
        })
    }

    fun pauseDownload(attachment: SceytAttachment) {
        downloadCalls[attachment.messageTid]?.cancel()
        downloadCalls.remove(attachment.messageTid)
    }

    fun resumeDownload(attachment: SceytAttachment): Boolean {
        // Check if there's already a download in progress
        return if (downloadCalls.containsKey(attachment.messageTid)) {
            false // Already downloading
        } else {
            true // Can resume - the downloadFile method will handle partial download
        }
    }

    @Suppress("unused")
    fun cancelAllDownloads() {
        downloadCalls.values.forEach { it.cancel() }
        downloadCalls.clear()
    }

    @Suppress("unused")
    fun isDownloading(messageTid: Long): Boolean {
        return downloadCalls.containsKey(messageTid)
    }

    @Suppress("unused")
    fun getActiveDownloadsCount(): Int {
        return downloadCalls.size
    }

    @Suppress("unused")
    fun getActiveDownloads(): List<Long> {
        return downloadCalls.keys.toList()
    }

    @Suppress("unused")
    fun cancelDownload(messageTid: Long) {
        downloadCalls[messageTid]?.cancel()
        downloadCalls.remove(messageTid)
    }
} 