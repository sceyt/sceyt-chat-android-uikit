package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.text.SpannableStringBuilder
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.getFormattedBodyWithAttachments

open class DefaultMessageBodyWithAttachmentsFormatter : Formatter<MessageBodyFormatterAttributes> {
    override fun format(context: Context, from: MessageBodyFormatterAttributes): CharSequence {
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

