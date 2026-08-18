package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import com.sceyt.chatuikit.persistence.mappers.needsVideoThumbUpload
import com.sceyt.chatuikit.persistence.mappers.upsertVideoThumbUrlMetadata
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * Uploads the first frame of a video as a separate file, so a receiver can show a real preview
 * before downloading the video itself. The url is written to the attachment metadata under
 * [com.sceyt.chatuikit.data.constants.SceytConstants.VideoThumbUrl].
 *
 * This runs before the video upload, so the url is already part of the attachment by the time the
 * message is sent. A failure is not fatal: the video still uploads, and receivers fall back to the
 * blurred thumb which every video carries in its metadata.
 */
internal class VideoThumbUploader(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val thumbFileProvider: (Context, String) -> Result<File> = { ctx, path ->
        FileResizeUtil.getVideoThumbAsFile(ctx, path, THUMB_MAX_SIZE)
    },
) {

    suspend fun uploadAndApplyThumb(
        attachment: SceytAttachment,
        tasks: List<TransferTask>,
        checksumData: FileChecksumData?,
    ) {
        if (!attachment.needsVideoThumbUpload()) return

        // The same file uploaded with another message already has a thumb on the server
        val knownUrl = getVideoThumbUrl(checksumData?.metadata)?.takeIf { it.isNotBlank() }
        if (knownUrl != null) {
            applyThumbUrl(attachment, tasks, knownUrl)
            return
        }

        val sourcePath = attachment.thumbSourcePath ?: return
        val thumbFile = thumbFileProvider(context, sourcePath).getOrElse { error ->
            SceytLog.e(TAG, "Couldn't extract a video thumb from $sourcePath: ${error.message}")
            return
        }

        try {
            val url = uploadThumb(attachment, thumbFile)?.takeIf { it.isNotBlank() }
            if (url == null) {
                SceytLog.e(TAG, "Video thumb upload returned an empty url for $sourcePath")
                return
            }
            applyThumbUrl(attachment, tasks, url)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            SceytLog.e(TAG, "Couldn't upload a video thumb for $sourcePath: ${error.message}")
        } finally {
            thumbFile.delete()
        }
    }

    private suspend fun uploadThumb(
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

    private suspend fun applyThumbUrl(
        attachment: SceytAttachment,
        tasks: List<TransferTask>,
        url: String,
    ) {
        tasks.forEach { task ->
            val metadata = task.attachment.upsertVideoThumbUrlMetadata(url) ?: return@forEach
            task.updateAttachmentAndStateIfValid(
                validate = { true },
                update = { it.copy(metadata = metadata) }
            )
            attachmentLogic.updateAttachmentMetadata(task.messageTid, metadata)
        }

        attachment.thumbSourcePath?.let { path ->
            val metadata = attachment.upsertVideoThumbUrlMetadata(url) ?: return@let
            attachmentLogic.updateFileChecksumMetadata(path, metadata)
        }
    }

    private val SceytAttachment.thumbSourcePath: String?
        get() = originalFilePath?.takeIf { it.isNotBlank() } ?: filePath?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "VideoThumbUploader"
        const val THUMB_MAX_SIZE = 600f
    }
}