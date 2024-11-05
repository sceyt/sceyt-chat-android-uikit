package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle

typealias MessageBodyFormatterAttributes = BodyFormatterAttributes<SceytMessage>
typealias DraftMessageBodyFormatterAttributes = BodyFormatterAttributes<DraftMessage>

data class BodyFormatterAttributes<Message>(
        val message: Message,
        val mentionTextStyle: TextStyle,
        val mentionClickListener: ((String) -> Unit)? = null,
        val attachmentNameFormatter: Formatter<SceytAttachment> = SceytChatUIKit.formatters.attachmentNameFormatter,
        val mentionUserNameFormatter: Formatter<SceytUser> = SceytChatUIKit.formatters.mentionUserNameFormatter,
)
