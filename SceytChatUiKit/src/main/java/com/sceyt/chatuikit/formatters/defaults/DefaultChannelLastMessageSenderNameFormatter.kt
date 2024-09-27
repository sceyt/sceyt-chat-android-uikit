package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.extensions.isSelf

data object DefaultChannelLastMessageSenderNameFormatter : Formatter<SceytChannel> {

    override fun format(context: Context, from: SceytChannel): CharSequence {
        val message = from.lastMessage ?: return ""
        return when {
            message.incoming -> {
                val sender = from.lastMessage.user
                val userFirstName = sender?.let {
                    SceytChatUIKit.formatters.userShortNameFormatter.format(context, it)
                }
                if (from.isGroup && !userFirstName.isNullOrBlank()) {
                    "${userFirstName}: "
                } else ""
            }

            from.isSelf() -> ""
            else -> "${context.getString(R.string.sceyt_your_last_message)}: "
        }
    }
}
