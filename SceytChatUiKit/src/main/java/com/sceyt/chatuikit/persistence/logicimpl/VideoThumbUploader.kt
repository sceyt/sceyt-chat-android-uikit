package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.needsVideoThumbUpload
import com.sceyt.chatuikit.persistence.mappers.upsertVideoThumbUrlMetadata
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Extracts the first frame of a video attachment and uploads it as a separate file,
 * in parallel with the video itself. The url of the uploaded frame is attached to the video
 * attachment metadata with the [com.sceyt.chatuikit.data.constants.SceytConstants.VideoThumbUrl] key.
 *
 * The video is reported as uploaded only if its thumb was uploaded as well, otherwise the attachment
 * goes to the error state, keeping the already uploaded video url, so a retry uploads only the
 * missing part.
 *
 * Uploads are keyed by the original file path, so a file which is shared with multiple messages
 * has a single thumb upload.
 */
internal class VideoThumbUploader(
    context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val transferUtility: FileTransferUtility,
    private val thumbFileProvider: (path: String) -> Result<File> = { path ->
        FileResizeUtil.getVideoThumbAsFile(context, path, THUMB_MAX_SIZE)
    },
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uploads = ConcurrentHashMap<String, CompletableDeferred<Result<String>>>()

    /**
     * Starts extracting and uploading the video thumb, if the attachment needs it,
     * and it's not already started.
     */
    fun start(attachment: SceytAttachment) {
        if (!attachment.needsVideoThumbUpload()) return
        val path = attachment.thumbSourcePath ?: return
        val deferred = CompletableDeferred<Result<String>>()
        // Do nothing if the thumb upload is already in progress or finished
        if (uploads.putIfAbsent(attachment.thumbKey, deferred) != null) return

        scope.launch {
            val thumbFile = thumbFileProvider(path).getOrElse {
                SceytLog.e(TAG, "Couldn't get a video thumb file for path: $path, error: ${it.message}")
                deferred.complete(Result.failure(it))
                return@launch
            }

            transferUtility.uploadFileByPath(attachment.thumbKey, thumbFile.path) { response ->
                thumbFile.delete()
                deferred.complete(response.fold(
                    onSuccess = { url ->
                        if (url.isNullOrBlank())
                            Result.failure(Throwable("Video thumb upload returned an empty url"))
                        else Result.success(url)
                    },
                    onError = { exception ->
                        Result.failure(Throwable(exception?.message ?: "Couldn't upload a video thumb"))
                    }
                ))
            }
        }
    }

    /**
     * Delivers the file upload result to the [tasks], waiting for the video thumb upload result
     * before reporting a success. The [tasks] are the tasks of every message which shares the file.
     */
    fun deliverResult(
        attachment: SceytAttachment,
        tasks: List<TransferTask>,
        response: SceytResponse<String>,
        isPaused: (messageTid: Long) -> Boolean = { false },
        onDelivered: () -> Unit = {},
    ) {
        val uploadedUrl = (response as? SceytResponse.Success)?.data?.takeIf { it.isNotBlank() }
        if (uploadedUrl == null || !attachment.needsVideoThumbUpload()) {
            tasks.forEach { it.uploadResultCallback?.onResult(response) }
            onDelivered()
            return
        }

        // Store the url before the thumb result, so a failed thumb upload
        // doesn't force uploading the video again
        tasks.forEach { task -> persistUploadedUrl(task, uploadedUrl) }
        // No-op if the thumb upload is already in progress
        start(attachment)

        await(attachment.thumbKey) { result ->
            result.onSuccess { thumbUrl ->
                tasks.forEach { task ->
                    if (isPaused(task.messageTid)) return@forEach
                    applyThumbUrl(task, thumbUrl)
                    task.uploadResultCallback?.onResult(SceytResponse.Success(uploadedUrl))
                }
            }.onFailure { error ->
                SceytLog.e(
                    TAG, "Couldn't upload a video thumb for ${attachment.thumbKey}," +
                            " reason: ${error.message}"
                )
                tasks.forEach { task ->
                    if (isPaused(task.messageTid)) return@forEach
                    task.uploadResultCallback?.onResult(
                        SceytResponse.Error(SceytException(0, error.message))
                    )
                }
            }
            onDelivered()
        }
    }

    fun cancel(attachment: SceytAttachment) {
        uploads.remove(attachment.thumbKey)?.cancel()
        transferUtility.cancelUploadFileByPath(attachment.thumbKey)
    }

    fun clear() {
        uploads.keys.toList().forEach { key ->
            uploads.remove(key)?.cancel()
            transferUtility.cancelUploadFileByPath(key)
        }
    }

    private fun await(key: String, callback: (Result<String>) -> Unit) {
        val deferred = uploads[key] ?: run {
            callback(Result.failure(Throwable("Video thumb upload was not started for key: $key")))
            return
        }
        scope.launch {
            val result = deferred.await()
            uploads.remove(key, deferred)
            callback(result)
        }
    }

    private fun persistUploadedUrl(task: TransferTask, url: String) {
        val attachment = task.attachment
        task.updateAttachmentAndStateIfValid(
            validate = { true },
            update = { it.copy(url = url) }
        )
        scope.launch {
            attachmentLogic.updateAttachmentUrl(attachment.messageTid, url)
            attachmentLogic.updateFileChecksumUrl(attachment.originalFilePath, url)
        }
    }

    private fun applyThumbUrl(task: TransferTask, thumbUrl: String) {
        var newMetadata: String? = null
        task.updateAttachmentAndStateIfValid(
            validate = { true },
            update = { current ->
                newMetadata = current.upsertVideoThumbUrlMetadata(thumbUrl) ?: current.metadata
                current.copy(metadata = newMetadata)
            }
        )
        val metadata = newMetadata ?: return
        val attachment = task.attachment
        scope.launch {
            attachmentLogic.updateAttachmentMetadata(attachment.messageTid, metadata)
            attachmentLogic.updateFileChecksumMetadata(attachment.originalFilePath, metadata)
        }
    }

    private val SceytAttachment.thumbSourcePath: String?
        get() = originalFilePath?.takeIf { it.isNotBlank() } ?: filePath?.takeIf { it.isNotBlank() }

    private val SceytAttachment.thumbKey: String
        get() = thumbSourcePath ?: messageTid.toString()

    companion object {
        private const val TAG = "VideoThumbUploader"
        private const val THUMB_MAX_SIZE = 600f
    }
}