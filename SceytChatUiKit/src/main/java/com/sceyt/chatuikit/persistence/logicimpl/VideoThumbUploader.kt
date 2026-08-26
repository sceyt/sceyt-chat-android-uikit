package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.needsVideoThumbUpload
import com.sceyt.chatuikit.persistence.mappers.upsertVideoThumbUrlMetadata
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Uploads the first frame of a video as a separate file, so a receiver can show a real preview
 * before downloading the video itself. The url is written to the attachment metadata under
 * [com.sceyt.chatuikit.data.constants.SceytConstants.VideoThumbUrl].
 *
 * This runs beside the video transfer. Failed uploads are retried while the video is still being
 * processed or uploaded. The caller cancels this work when the video finishes, so a missing poster
 * never fails or delays the message.
 */
internal class VideoThumbUploader(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val thumbFileProvider: (Context, String) -> Result<File> = { ctx, path ->
        FileResizeUtil.getVideoThumbAsFile(ctx, path, THUMB_MAX_SIZE)
    },
    private val retryDelay: Duration = RETRY_DELAY,
) {

    suspend fun uploadThumb(attachment: SceytAttachment): String? {
        if (!attachment.needsVideoThumbUpload()) return null

        val sourcePath = attachment.thumbSourcePath ?: return null
        val thumbFile = thumbFileProvider(context, sourcePath).getOrElse { error ->
            SceytLog.e(TAG, "Couldn't extract a video thumb from $sourcePath: ${error.message}")
            return null
        }

        try {
            while (true) {
                try {
                    return uploadThumbFile(attachment, thumbFile)?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("Video thumb upload returned an empty url")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    SceytLog.e(
                        TAG,
                        "Couldn't upload a video thumb for $sourcePath: ${error.message}",
                    )
                    delay(retryDelay)
                }
            }
        } finally {
            thumbFile.delete()
        }
    }

    private suspend fun uploadThumbFile(
        attachment: SceytAttachment,
        thumbFile: File,
    ): String? {
        val request = FileUploadRequest(
            operationId = uploadOperationId(attachment.messageTid, TransferRole.Thumbnail),
            sourceFile = thumbFile,
            fileName = thumbFile.name,
            mimeType = getMimeType(thumbFile.path),
            attachment = attachment,
            isSharedUpload = false,
            role = TransferRole.Thumbnail,
        )

        return SceytChatUIKit.fileTransfer.transport.upload(request) {}
    }

    suspend fun applyThumbUrl(
        attachment: SceytAttachment,
        tasks: List<TransferTask>,
        url: String,
    ) {
        tasks.forEach { task ->
            try {
                val metadata = task.attachment.upsertVideoThumbUrlMetadata(url) ?: return@forEach
                task.updateAttachmentAndStateIfValid(
                    validate = { true },
                    update = { it.copy(metadata = metadata) }
                )
                attachmentLogic.updateAttachmentMetadata(task.messageTid, metadata)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                SceytLog.e(TAG, "Couldn't persist a video thumb for ${task.messageTid}", error)
            }
        }

        attachment.thumbSourcePath?.let { path ->
            try {
                val metadata = attachment.upsertVideoThumbUrlMetadata(url) ?: return@let
                attachmentLogic.updateFileChecksumMetadata(path, metadata)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                SceytLog.e(TAG, "Couldn't persist video thumb checksum metadata for $path", error)
            }
        }
    }

    private val SceytAttachment.thumbSourcePath: String?
        get() = originalFilePath?.takeIf { it.isNotBlank() } ?: filePath?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "VideoThumbUploader"
        const val THUMB_MAX_SIZE = 600f
        val RETRY_DELAY = 1.seconds
    }
}
