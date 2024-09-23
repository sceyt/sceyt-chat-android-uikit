package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.presentation.extensions.isTextMessage

open class DefaultReplyMessageBodyFormatter : Formatter<SceytMessage> {
    override fun format(context: Context, from: SceytMessage): CharSequence {
        return if (from.isTextMessage())
            MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(from)
        else from.getFormattedBody(context)
    }
}