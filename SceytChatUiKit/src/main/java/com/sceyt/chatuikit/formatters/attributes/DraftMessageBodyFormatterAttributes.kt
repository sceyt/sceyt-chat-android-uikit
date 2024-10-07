package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle

data class DraftMessageBodyFormatterAttributes(
        val message: DraftMessage,
        val mentionTextStyle: TextStyle,
        val mentionClickListener: ((String) -> Unit)? = null,
        val attachmentNameFormatter: Formatter<SceytAttachment> = SceytChatUIKit.formatters.attachmentNameFormatter,
        val mentionUserNameFormatter: Formatter<SceytUser> = SceytChatUIKit.formatters.mentionUserNameFormatter,
)
