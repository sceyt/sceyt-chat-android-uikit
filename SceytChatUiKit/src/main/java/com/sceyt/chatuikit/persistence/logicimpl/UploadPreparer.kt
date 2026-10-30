package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.transcodeVideo
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal data class PreparedAttachment(
    val attachment: SceytAttachment,
    val reusedUrl: String? = null,
)

/** Prepares a source file without owning transfer tasks, upload ordering, or sharing. */
internal class UploadPreparer(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
) {
    suspend fun prepare(
        attachment: SceytAttachment,
        onFilePrepared: (String) -> Unit,
        onProgress: (Float) -> Unit,
        reuseUploadedUrl: Boolean = true,
    ): PreparedAttachment {
        val sourcePath = attachment.originalFilePath?.takeIf(String::isNotBlank)
            ?: attachment.filePath?.takeIf(String::isNotBlank)
        val checksum = if (reuseUploadedUrl && SceytChatUIKit.config.preventDuplicateAttachmentUpload)
            attachmentLogic.getFileChecksumData(sourcePath) else null
        if (checksum?.url.isNotNullOrBlank()) {
            if (!checksum?.resizedFilePath.isNullOrEmpty()) {
                onFilePrepared(checkNotNull(checksum.resizedFilePath))
            }
            return PreparedAttachment(attachment, checkNotNull(checksum?.url))
        }

        val result = prepareAttachment(attachment, checksum, onProgress)
        currentCoroutineContext().ensureActive()
        val prepared = result.fold(
            onSuccess = { path ->
                onFilePrepared(path)
                attachment.copy(filePath = path, fileSize = getFileSize(path))
            },
            onFailure = {
                SceytLog.i(TAG, "Couldn't resize file with reason ${it.message}")
                attachment
            },
        )
        val path = prepared.filePath
        require(!path.isNullOrBlank()) { "Attachment source path is missing" }
        require(File(path).isFile) { "Attachment source file does not exist: $path" }
        return PreparedAttachment(prepared)
    }

    private suspend fun prepareAttachment(
        attachment: SceytAttachment,
        checksum: FileChecksumData?,
        onProgress: (Float) -> Unit,
    ): Result<String> {
        val resizedPath = checksum?.resizedFilePath
        if (resizedPath != null && File(resizedPath).exists()) return Result.success(resizedPath)

        return when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                val config = SceytChatUIKit.config.imageAttachmentResizeConfig
                resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = config.dimensionThreshold,
                    quality = config.compressionQuality,
                )
            }
            AttachmentTypeEnum.Video.value -> transcodeAttachment(attachment, onProgress)
            else -> Result.failure(Exception("Unsupported attachment type: ${attachment.type}"))
        }
    }

    private suspend fun transcodeAttachment(
        attachment: SceytAttachment,
        onProgress: (Float) -> Unit,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        val destination = transcodeVideo(
            path = attachment.filePath,
            parentDir = context.filesDir,
            config = SceytChatUIKit.config.videoAttachmentResizeConfig,
            progressCallback = { data ->
                if (continuation.isActive) onProgress(data.progressPercent)
            },
        ) { result ->
            if (completed.compareAndSet(false, true)) continuation.resume(result)
        }
        continuation.invokeOnCancellation {
            completed.set(true)
            destination?.let(VideoTranscodeHelper::cancel)
        }
    }

    private companion object {
        const val TAG = "UploadPreparer"
    }
}
