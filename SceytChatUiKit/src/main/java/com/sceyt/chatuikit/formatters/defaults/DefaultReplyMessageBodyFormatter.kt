package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.graphics.Typeface
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.styles.common.TextStyle

open class DefaultReplyMessageBodyFormatter : Formatter<SceytMessage> {
    override fun format(context: Context, from: SceytMessage): CharSequence {
        return from.getFormattedBody(context, TextStyle(
            style = Typeface.BOLD,
        ))
    }
}