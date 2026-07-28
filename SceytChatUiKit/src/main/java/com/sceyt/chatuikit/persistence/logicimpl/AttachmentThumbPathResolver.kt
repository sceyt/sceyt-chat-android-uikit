package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import java.io.FileNotFoundException
import kotlin.math.max

internal object AttachmentThumbPathResolver : ThumbPathResolver {

    override fun getThumbPath(
        context: Context,
        attachment: SceytAttachment,
        size: Size,
    ): Result<String> {
        val path = attachment.filePath ?: return Result.failure(FileNotFoundException())
        val minSize = max(size.height, size.width)
        val reqSize = if (minSize > 0) minSize.toFloat() else 800f
        return when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                FileResizeUtil.getImageThumbAsFile(context, path, reqSize).map { it.path }
            }

            AttachmentTypeEnum.Video.value -> {
                FileResizeUtil.getVideoThumbAsFile(context, path, reqSize).map { it.path }
            }

            else -> Result.failure(Exception("Unsupported attachment type: ${attachment.type}"))
        }
    }
}
