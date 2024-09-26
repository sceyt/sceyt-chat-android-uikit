package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.extensions.getShowName

open class DefaultAttachmentNameFormatter : Formatter<SceytAttachment> {
    override fun format(context: Context, from: SceytAttachment): CharSequence {
        return from.getShowName(context)
    }
}