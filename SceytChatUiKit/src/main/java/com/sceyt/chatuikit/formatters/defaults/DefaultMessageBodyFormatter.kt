package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody

open class DefaultMessageBodyFormatter : Formatter<SceytMessage> {
    override fun format(context: Context, from: SceytMessage): CharSequence {
        return from.getFormattedBody(context)
    }
}