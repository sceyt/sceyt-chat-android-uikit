package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        var id: Long,
        var type: ChannelTypeEnum,
        var createdAt: Long,
        var updatedAt: Long,
        var unreadMessageCount: Long,
        var unreadMentionCount: Long,
        var unreadReactionCount: Long,
        var lastMessageTid: Long?,
        var lastMessageAt: Long?,
        var label: String?,
        var metadata: String?,
        var muted: Boolean,
        var muteExpireDate: Long?,
        var markedUsUnread: Boolean,
        var subject: String?,
        var channelUrl: String?,
        var avatarUrl: String?,
        var memberCount: Long,
        var lastDeliveredMessageId: Long,
        var lastReadMessageId: Long,
        var messagesDeletionDate: Long,
        var role: String?
)