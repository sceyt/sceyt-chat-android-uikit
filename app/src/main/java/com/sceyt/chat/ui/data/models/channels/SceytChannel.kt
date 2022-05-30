package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.message.Message
import java.util.*

open class SceytChannel(var id: Long,
                        var createdAt: Long,
                        var updatedAt: Long,
                        var unreadMessageCount: Long,
                        var lastMessage: Message?,
                        var label: String?,
                        var metadata: String?,
                        var muted: Boolean,
                        var muteExpireDate: Date?,
                        var channelType: ChannelTypeEnum?) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytChannel) return false
        return (other.id == id && other.unreadMessageCount == unreadMessageCount
                && other.lastMessage?.id == lastMessage?.id && other.channelType == channelType)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + unreadMessageCount.hashCode()
        result = 31 * result + (lastMessage?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + muted.hashCode()
        result = 31 * result + (muteExpireDate?.hashCode() ?: 0)
        result = 31 * result + (channelType?.hashCode() ?: 0)
        return result
    }
}