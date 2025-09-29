package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.getFormattedBodyWithAttachments

open class DefaultDraftMessageBodyWithAttachmentsFormatter : Formatter<DraftMessageBodyFormatterAttributes> {
    override fun format(context: Context, from: DraftMessageBodyFormatterAttributes): CharSequence {
        val message = from.message

        return message.getFormattedBodyWithAttachments(
            context = context,
            mentionTextStyle = from.mentionTextStyle,
            attachmentNameFormatter = from.attachmentNameFormatter,
            mentionUserNameFormatter = from.mentionUserNameFormatter,
            mentionClickListener = from.mentionClickListener,
        )
    }
}
