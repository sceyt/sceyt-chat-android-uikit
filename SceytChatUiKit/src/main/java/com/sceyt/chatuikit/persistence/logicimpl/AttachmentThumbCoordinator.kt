package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

internal class AttachmentThumbCoordinator(
    private val context: Context,
    private val thumbPathResolver: ThumbPathResolver,
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val thumbPaths = ConcurrentHashMap<String, ThumbPathData>()
    private val preparingThumbs = ConcurrentHashMap<String, ThumbData>()

    fun getAttachmentThumb(
        messageTid: Long,
        attachment: SceytAttachment,
        data: ThumbData,
    ) {
        attachment.filePath ?: return
        val thumbKey = getThumbSourceKey(attachment, data)
        val preparingThumbKey = "${attachment.messageTid}_${thumbKey}_${data.key}"

        val task = fileTransferService.findTransferTask(attachment)
            ?: FileTransferHelper.createTransferTask(attachment)
        val readyThumb = thumbPaths[thumbKey]

        if (readyThumb != null) {
            task.thumbCallback?.onThumb(readyThumb.path, data)
            return
        }

        if (preparingThumbs.put(preparingThumbKey, data) != null) return

        thumbPathResolver.getThumbPath(context, attachment, data.size)
            .onSuccess { path ->
                thumbPaths[thumbKey] = ThumbPathData(messageTid, path, data.size)

                task.thumbCallback?.onThumb(
                    path = path,
                    thumbData = preparingThumbs.remove(preparingThumbKey) ?: data,
                )
            }.onFailure {
                preparingThumbs.remove(preparingThumbKey)

                SceytLog.e(
                    TAG,
                    "Couldn't get a thumb for messageTid: $messageTid," +
                            " path:${attachment.filePath} with reason ${it.message}",
                )
            }
    }

    fun clearPreparingThumbPaths() {
        preparingThumbs.clear()
    }

    fun clear() {
        preparingThumbs.clear()
        thumbPaths.clear()
    }

    private fun getThumbSourceKey(
        attachment: SceytAttachment,
        data: ThumbData,
    ): String {
        val path = if (attachment.originalFilePath.isNullOrBlank()) {
            attachment.filePath ?: data.filePath
        } else {
            attachment.originalFilePath
        }

        return "${path}_${data.size}"
    }

    private data class ThumbPathData(
        val messageTid: Long,
        val path: String,
        val size: Size,
    )

    private companion object {
        const val TAG = "FileTransferLogic"
    }
}
