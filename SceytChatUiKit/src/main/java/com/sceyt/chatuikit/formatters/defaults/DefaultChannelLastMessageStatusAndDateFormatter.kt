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

        val (timestamp, shouldShowStatus) = when {
            channel.draftMessage != null -> {
                channel.draftMessage.createdAt to false
            }

            channel.lastMessage != null -> {
                val lastMessage = channel.lastMessage
                lastMessage.createdAt to !lastMessage.isSystemMessage()
            }

            else -> channel.createdAt to true
        }

        val dateText = from.channelItemStyle.channelDateFormatter.format(
            context,
            Date(timestamp)
        )

        return ChannelLastMessageStatusAndDate(
            dateText = dateText,
            shouldShowStatus = shouldShowStatus
        )
    }
}