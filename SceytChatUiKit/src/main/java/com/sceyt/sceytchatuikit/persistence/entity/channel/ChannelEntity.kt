package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        var id: Long,
        var type: ChannelTypeEnum,
        var createdAt: Long,
        var updatedAt: Long,
        var unreadMessageCount: Long,
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
        var myRole: RoleTypeEnum
)