package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes

open class DefaultUnsupportedMessageBodyFormatter : Formatter<MessageBodyFormatterAttributes> {
    override fun format(context: Context, from: MessageBodyFormatterAttributes): CharSequence {
        return context.getString(R.string.unsupported_message_text)
    }
}