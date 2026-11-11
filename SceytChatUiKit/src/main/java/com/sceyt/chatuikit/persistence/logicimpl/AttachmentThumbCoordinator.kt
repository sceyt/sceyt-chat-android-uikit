package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.takeIfNotBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.isCompleted
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

internal class AttachmentThumbCoordinator(
    private val context: Context,
    private val thumbPathResolver: ThumbPathResolver,
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val previewTransferLazy: Lazy<AttachmentPreviewTransfer> = inject()
    private val previewTransfer by previewTransferLazy
    private val thumbPaths = ConcurrentHashMap<String, ThumbPathData>()
    private val preparingThumbs = ConcurrentHashMap<String, ThumbData>()

    fun getAttachmentThumb(
        messageTid: Long,
        attachment: SceytAttachment,
        data: ThumbData,
    ) {
        if (attachment.isVideoDownloadPending()) {
            val task = fileTransferService.findTransferTask(attachment)
                ?: FileTransferHelper.createTransferTask(attachment)
            previewTransfer.loadPreviewPath(attachment) { path ->
                task.thumbCallback?.onThumb(path, data)
            }
            return
        }

        // Thumbnail generation needs a local source before creating a task or calling the resolver.
        attachment.filePath ?: return

        val thumbKey = getThumbSourceKey(attachment, data)
        val preparingThumbKey = "${attachment.messageTid}_${thumbKey}_${data.key}"

        val task = fileTransferService.findTransferTask(attachment)
            ?: FileTransferHelper.createTransferTask(attachment)
        val cachedThumb = thumbPaths[thumbKey]

        if (cachedThumb != null && File(cachedThumb.path).exists()) {
            // Messages with the same source and requested size can share the cached thumbnail.
            task.thumbCallback?.onThumb(cachedThumb.path, data)
            return
        }

        if (cachedThumb != null) {
            SceytLog.w(
                TAG, "Cached thumb file is missing, regenerating for messageTid: $messageTid," +
                        " thumb:${cachedThumb.path}, source:${attachment.filePath}"
            )
            thumbPaths.remove(thumbKey, cachedThumb)
        }

        // Keep the latest target details while an existing request finishes generating its thumbnail.
        if (preparingThumbs.put(preparingThumbKey, data) != null) return

        thumbPathResolver.getThumbPath(context, attachment, data.size)
            .onSuccess { path ->
                thumbPaths[thumbKey] = ThumbPathData(messageTid, path, data.size)

                task.thumbCallback?.onThumb(
                    path = path,
                    thumbData = preparingThumbs.remove(preparingThumbKey) ?: data,
                )
            }.onFailure { error ->
                preparingThumbs.remove(preparingThumbKey)
                if (error is FileNotFoundException && attachment.canRedownload()) {
                    SceytLog.w(
                        TAG,
                        "Original file is missing, downloading again for messageTid: $messageTid," +
                                " path:${attachment.filePath}, url:${attachment.url}"
                    )
                    fileTransferService.redownload(attachment)
                }
                SceytLog.e(
                    TAG,
                    "Couldn't get a thumb for messageTid: $messageTid," +
                            " path:${attachment.filePath} with reason ${error.message}",
                )
            }
    }

    fun clearPreparingThumbPaths() {
        preparingThumbs.clear()
    }

    fun clear() {
        preparingThumbs.clear()
        thumbPaths.clear()
        if (previewTransferLazy.isInitialized()) previewTransfer.cancelAll()
    }

    private fun SceytAttachment.isVideoDownloadPending(): Boolean {
        if (type != AttachmentTypeEnum.Video.value) return false
        return when (transferState) {
            TransferState.PendingDownload, TransferState.Downloading,
            TransferState.PauseDownload, TransferState.ErrorDownload -> true

            else -> false
        }
    }

    private fun SceytAttachment.canRedownload(): Boolean {
        // Automatic recovery applies to completed attachments with a remote URL and a missing local file.
        val path = filePath ?: return false
        if (url.isNullOrBlank() || transferState?.isCompleted() != true) return false
        return !File(path).exists()
    }

    private fun getThumbSourceKey(
        attachment: SceytAttachment,
        data: ThumbData,
    ): String {
        val path = attachment.originalFilePath.takeIfNotBlank()
            ?: attachment.filePath
            ?: data.filePath

        return "${path}_${data.size}"
    }

    private data class ThumbPathData(
        val messageTid: Long,
        val path: String,
        val size: Size,
    )

    private companion object {
        const val TAG = "AttachmentThumbCoordinator"
    }
}
