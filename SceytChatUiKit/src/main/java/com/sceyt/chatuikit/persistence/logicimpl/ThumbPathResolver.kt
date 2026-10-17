package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.SceytAttachment

internal fun interface ThumbPathResolver {
    fun getThumbPath(context: Context, attachment: SceytAttachment, size: Size): Result<String>
}
