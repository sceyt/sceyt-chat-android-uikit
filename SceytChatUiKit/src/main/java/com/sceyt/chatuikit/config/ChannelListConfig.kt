package com.sceyt.chatuikit.config

import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel

/**
 * Configuration for the channel list.
 * @param types List of channel types to be displayed in the list. If empty, all types will be displayed.
 * @param order Order of the channels in the list.
 * @param queryLimit Limit of channels to be queried.
 * @param queryParam Query parameters for the channel list.
 * */
data class ChannelListConfig(
        val types: List<String>,
        val order: ChannelListOrder,
        val queryLimit: Int,
        val queryParam: ChannelQueryParam,
) {

    override fun hashCode(): Int {
        return types.toSet().sorted().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    fun isValidForConfig(channel: SceytChannel): Boolean {
        return !(types.isNotEmpty() && !types.contains(channel.type))
    }

    companion object {
        val default = ChannelListConfig(
            types = emptyList(),
            order = ChannelListOrder.ListQueryChannelOrderLastMessage,
            queryLimit = SceytChatUIKit.config.queryLimits.channelListQueryLimit,
            queryParam = ChannelQueryParam(1, 10, 1, true)
        )
    }
}