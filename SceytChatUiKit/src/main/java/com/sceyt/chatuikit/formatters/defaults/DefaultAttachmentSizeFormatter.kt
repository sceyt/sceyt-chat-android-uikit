package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.formatters.Formatter

class DefaultAttachmentSizeFormatter : Formatter<SceytAttachment> {
    override fun format(context: Context, from: SceytAttachment): CharSequence {
        val size = from.fileSize
        return if (size != 0L) {
            size.toPrettySize()
        } else getFileSize(from.filePath.toString()).toPrettySize()
    }
}