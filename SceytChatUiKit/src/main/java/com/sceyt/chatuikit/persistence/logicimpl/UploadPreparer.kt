package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.ifNotEmpty
import com.sceyt.chatuikit.extensions.takeIfNotBlank
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
    val preparedFilePath: String? = null,
)

/** Prepares a source file without owning transfer tasks, upload ordering, or sharing. */
internal class UploadPreparer(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
) {
    suspend fun prepare(
        attachment: SceytAttachment,
        onProgress: (Float) -> Unit,
        reuseUploadedUrl: Boolean = true,
        preparedFilePath: String? = null,
    ): PreparedAttachment {
        val originalPath = attachment.originalFilePath.takeIfNotBlank()
        val sourcePath = originalPath ?: attachment.filePath.takeIfNotBlank()

        val canReuseUrl = reuseUploadedUrl && SceytChatUIKit.config.preventDuplicateAttachmentUpload
        val canReusePreparedMedia = when (attachment.type) {
            AttachmentTypeEnum.Image.value, AttachmentTypeEnum.Video.value -> !reuseUploadedUrl
            else -> false
        }

        val preparedPath = if (canReusePreparedMedia)
            findPreparedFile(attachment, originalPath, preparedFilePath) else null

        // Look up a cached URL or a prepared media file missing from this attempt.
        val checksum = if (canReuseUrl || (canReusePreparedMedia && preparedPath == null)) {
            attachmentLogic.getFileChecksumData(sourcePath)
        } else null

        val reusedUrl = if (canReuseUrl) checksum?.url.takeIfNotBlank() else null
        if (reusedUrl != null) {
            // The cached URL completes preparation using the previously uploaded file.
            return PreparedAttachment(attachment, reusedUrl, checksum?.resizedFilePath.ifNotEmpty())
        }

        // Start from the original file when rebuilding encrypted media.
        val source = if (canReusePreparedMedia && attachment.filePath != sourcePath) {
            attachment.copy(filePath = sourcePath)
        } else attachment

        return prepareSource(source, checksum, preparedPath, onProgress)
    }

    private fun findPreparedFile(
        attachment: SceytAttachment,
        originalPath: String?,
        preparedFilePath: String?,
    ): String? {
        val savedPath = attachment.filePath?.takeIf { originalPath != null && it != originalPath }
        return listOfNotNull(preparedFilePath, savedPath).firstOrNull { File(it).isFile }
    }

    private suspend fun prepareSource(
        attachment: SceytAttachment,
        checksum: FileChecksumData?,
        preparedPath: String?,
        onProgress: (Float) -> Unit,
    ): PreparedAttachment {
        val preparationResult = preparedPath?.let { Result.success(it) }
            ?: prepareAttachment(attachment, checksum, onProgress)

        // Image resizing can finish after cancellation, so check before publishing its file path.
        currentCoroutineContext().ensureActive()

        val prepared = preparationResult.fold(
            onSuccess = { path ->
                attachment.copy(filePath = path, fileSize = getFileSize(path))
            },
            onFailure = {
                SceytLog.i(TAG, "Couldn't resize file with reason ${it.message}")
                attachment
            },
        )

        val path = prepared.filePath.takeIfNotBlank()
        requireNotNull(path) { "Attachment source path is missing" }
        require(File(path).isFile) { "Attachment source file does not exist: $path" }
        return PreparedAttachment(prepared, preparedFilePath = preparationResult.getOrNull())
    }

    private suspend fun prepareAttachment(
        attachment: SceytAttachment,
        checksum: FileChecksumData?,
        onProgress: (Float) -> Unit,
    ): Result<String> {
        val resizedPath = checksum?.resizedFilePath

        // Reuse an existing prepared file before running resize or transcode again.
        if (resizedPath != null && File(resizedPath).isFile) return Result.success(resizedPath)

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
            // Ignore duplicate terminal callbacks and results arriving after cancellation.
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
