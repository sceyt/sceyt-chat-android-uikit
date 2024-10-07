package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.styles.common.TextStyle

typealias MessageAndMentionTextStylePair = Pair<SceytMessage, TextStyle>

object DefaultMessageBodyFormatter : Formatter<MessageAndMentionTextStylePair> {
    override fun format(context: Context, from: MessageAndMentionTextStylePair): CharSequence {
        val (message, mentionTextStyle) = from
        return message.getFormattedBody(
            context = context,
            mentionTextStyle = mentionTextStyle,
            attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
            mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter
        )
    }
}
