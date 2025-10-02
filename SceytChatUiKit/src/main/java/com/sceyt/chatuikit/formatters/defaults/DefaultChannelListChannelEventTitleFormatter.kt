package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.ChannelEventTitleFormatterAttributes
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum

open class DefaultChannelListChannelEventTitleFormatter : Formatter<ChannelEventTitleFormatterAttributes> {

    override fun format(context: Context, from: ChannelEventTitleFormatterAttributes): String {
        if (from.users.isEmpty()) return ""
        val text = if (from.users.any { it.activity == ChannelEventEnum.Typing })
            context.getString(R.string.sceyt_typing_)
        else context.getString(R.string.recording_)
        return if (from.channel.isGroup) {
            buildString {
                from.users.forEachIndexed { index, activeUser ->
                    append(getFormattedName(context, activeUser.user))
                    if (index < from.users.lastIndex)
                        append(", ")
                }

                append(" $text")
            }
        } else text
    }

    private fun getFormattedName(context: Context, user: SceytUser): CharSequence {
        return SceytChatUIKit.formatters.channelEventUserNameFormatter.format(context, user)
    }
}

