package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyAttributeType
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper

open class DefaultMessageBodyFormatter : Formatter<MessageBodyFormatterAttributes> {
    override fun format(context: Context, from: MessageBodyFormatterAttributes): CharSequence {
        val message = from.message
        var body: CharSequence = message.body.trim()
        if (!message.bodyAttributes.isNullOrEmpty()) {
            body = MessageBodyStyleHelper.buildOnlyTextStyles(body, message.bodyAttributes)
            if (!message.mentionedUsers.isNullOrEmpty()) {
                body = MentionUserHelper.buildWithMentionedUsers(
                    context = context,
                    body = body,
                    mentionAttributes = message.bodyAttributes.filter {
                        it.type == BodyAttributeType.Mention.value
                    },
                    mentionUsers = message.mentionedUsers,
                    mentionTextStyle = from.mentionTextStyle,
                    mentionClickListener = {
                        from.mentionClickListener?.invoke(it)
                    },
                    mentionUserNameFormatter = from.mentionUserNameFormatter
                )
            }
        }
        return body
    }
}
