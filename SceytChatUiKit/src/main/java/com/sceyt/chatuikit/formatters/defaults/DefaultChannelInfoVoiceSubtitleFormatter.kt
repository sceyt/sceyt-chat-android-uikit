package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.formatters.Formatter
import java.util.Date

open class DefaultChannelInfoVoiceSubtitleFormatter : Formatter<SceytAttachment> {
    override fun format(context: Context, from: SceytAttachment): CharSequence {
        return SceytChatUIKit.formatters.channelInfoAttachmentDateFormatter.format(context, Date(from.createdAt))
    }
}