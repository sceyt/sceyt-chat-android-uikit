package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.mappers.getVisualMediaType
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import java.io.FileNotFoundException
import kotlin.math.max

internal object AttachmentThumbPathResolver : ThumbPathResolver {

    override fun getThumbPath(
        context: Context,
        attachment: SceytAttachment,
        size: Size,
    ): Result<String> {
        // Thumbnail generation needs a local source file.
        val path = attachment.filePath ?: return Result.failure(FileNotFoundException())
        val maxDimension = max(size.height, size.width)
        val requestedSize = if (maxDimension > 0) maxDimension.toFloat() else 800f

        return when (attachment.getVisualMediaType()) {
            AttachmentTypeEnum.Image -> {
                FileResizeUtil.getImageThumbAsFile(context, path, requestedSize).map { it.path }
            }

            AttachmentTypeEnum.Video -> {
                FileResizeUtil.getVideoThumbAsFile(context, path, requestedSize).map { it.path }
            }

            else -> Result.failure(Exception("Unsupported attachment type: ${attachment.type}"))
        }
    }
}
