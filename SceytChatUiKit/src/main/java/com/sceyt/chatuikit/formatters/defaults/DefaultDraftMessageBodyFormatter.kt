package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper

open class DefaultDraftMessageBodyFormatter : Formatter<DraftMessageBodyFormatterAttributes> {
    override fun format(context: Context, from: DraftMessageBodyFormatterAttributes): CharSequence {
        val message = from.message
        return MessageBodyStyleHelper.buildWithAttributes(
            context = context,
            body = message.body.toString(),
            mentionUsers = message.mentionUsers,
            bodyAttributes = message.bodyAttributes,
            mentionTextStyle = from.mentionTextStyle,
            mentionUserNameFormatter = from.mentionUserNameFormatter,
            mentionClickListener = from.mentionClickListener,
        )
    }
}

