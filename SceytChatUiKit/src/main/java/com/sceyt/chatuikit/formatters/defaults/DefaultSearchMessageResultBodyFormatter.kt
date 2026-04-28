package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import androidx.core.text.buildSpannedString
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.isSupportedType

open class DefaultSearchMessageResultBodyFormatter :
    Formatter<SearchMessageResultFormatterAttributes> {

    override fun format(
        context: Context,
        from: SearchMessageResultFormatterAttributes
    ): CharSequence {
        val channel = from.channel
        val message = from.message
        val resultStyle = from.searchMessageItemStyle

        if (message.type == SceytMessageType.System.value) {
            return SceytChatUIKit.formatters.systemMessageBodyFormatter.format(context, message)
        }

        if (!message.isSupportedType()) {
            return SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter.format(
                context = context,
                from = message
            )
        }

        val body = resultStyle.messageBodyFormatter.format(
            context, MessageBodyFormatterAttributes(
                message = message,
                mentionTextStyle = resultStyle.mentionTextStyle
            )
        )

        if (message.type == SceytMessageType.ViewOnce.value) {
            return body
        }

        val shouldShowSenderName = !channel.isSelf
        val senderName = if (shouldShowSenderName) {
            resultStyle.senderNameFormatter.format(context, message)
        } else ""

        val attachmentIcon = message.attachments?.firstOrNull()?.let {
            resultStyle.attachmentIconProvider.provide(context, it)
        }

        return buildSpannedString {
            if (senderName.isNotEmpty()) {
                append(senderName)
                resultStyle.lastMessageSenderNameTextStyle.apply(
                    context = context,
                    spannable = this,
                    start = 0,
                    end = senderName.length
                )
            }
            append(attachmentIcon.toSpannableString())
            append(body)
        }
    }
}