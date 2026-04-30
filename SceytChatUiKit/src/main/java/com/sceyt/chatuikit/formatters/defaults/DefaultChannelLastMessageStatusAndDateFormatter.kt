package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.ChannelLastMessageStatusAndDate
import com.sceyt.chatuikit.formatters.TypedFormatter
import com.sceyt.chatuikit.formatters.attributes.ChannelLastMessageStatusAndDateFormatterAttributes
import com.sceyt.chatuikit.presentation.extensions.isSystemMessage
import java.util.Date

open class DefaultChannelLastMessageStatusAndDateFormatter :
    TypedFormatter<ChannelLastMessageStatusAndDateFormatterAttributes, ChannelLastMessageStatusAndDate> {

    override fun format(
        context: Context,
        from: ChannelLastMessageStatusAndDateFormatterAttributes,
    ): ChannelLastMessageStatusAndDate {
        val channel = from.channel
        var shouldShowStatus = true
        val timestamp = when {
            channel.draftMessage != null -> {
                shouldShowStatus = false
                channel.draftMessage.createdAt
            }

            channel.lastMessage != null -> {
                shouldShowStatus = !channel.lastMessage.isSystemMessage()
                val lastMessageCreatedAt = channel.lastMessage.createdAt
                val lastReactionCreatedAt =
                    channel.newReactions?.maxByOrNull { it.id }?.createdAt ?: 0
                if (lastReactionCreatedAt > lastMessageCreatedAt) lastReactionCreatedAt
                else lastMessageCreatedAt
            }

            else -> channel.createdAt
        }
        val dateText = from.channelItemStyle.channelDateFormatter.format(context, Date(timestamp))
        return ChannelLastMessageStatusAndDate(dateText, shouldShowStatus)
    }
}
