package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.getFormattedBodyWithAttachments

open class DefaultMessageBodyWithAttachmentsFormatter : Formatter<MessageBodyFormatterAttributes> {
    override fun format(context: Context, from: MessageBodyFormatterAttributes): CharSequence {
        return from.message.getFormattedBodyWithAttachments(
            context = context,
            mentionTextStyle = from.mentionTextStyle,
            messageTypeIconProvider = from.messageTypeIconProvider,
            mentionUserNameFormatter = from.mentionUserNameFormatter,
            attachmentNameFormatter = from.attachmentNameFormatter,
            mentionClickListener = from.mentionClickListener
        )
    }
}

