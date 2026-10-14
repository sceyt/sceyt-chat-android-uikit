package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle

data class PinnedMessageBodyFormatterAttributes(
    val message: SceytMessage,
    val mentionTextStyle: TextStyle,
    val deletedStateText: CharSequence,
    val mentionUserNameFormatter: Formatter<SceytUser> =
        SceytChatUIKit.formatters.mentionUserNameFormatter,
    val unsupportedMessageShortBodyFormatter: Formatter<SceytMessage> =
        SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter,
    val voiceDurationFormatter: Formatter<Long> = SceytChatUIKit.formatters.voiceDurationFormatter,
)