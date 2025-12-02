package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.getFormattedBodyWithAttachments
import com.sceyt.chatuikit.presentation.extensions.isSupportedType

open class DefaultMessageBodyWithAttachmentsFormatter : Formatter<MessageBodyFormatterAttributes> {
    override fun format(context: Context, from: MessageBodyFormatterAttributes): CharSequence {
        if (from.message.type == SceytMessageType.System.value) {
            return SceytChatUIKit.formatters.systemMessageBodyFormatter.format(context, from.message)
        }

        if (!from.message.isSupportedType()){
            val text = SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter.format(context, from.message)
            return text
        }

        val body = from.message.getFormattedBodyWithAttachments(
            context = context,
            mentionTextStyle = from.mentionTextStyle,
            mentionUserNameFormatter = from.mentionUserNameFormatter,
            attachmentNameFormatter = from.attachmentNameFormatter,
            mentionClickListener = from.mentionClickListener
        )

        val messageTypeIcon = from.messageTypeIconProvider.provide(context, from.message)
        return if (messageTypeIcon != null) {
            SpannableStringBuilder().apply {
                append(messageTypeIcon.toSpannableString())
                append(body)
            }
        } else body
    }
}

