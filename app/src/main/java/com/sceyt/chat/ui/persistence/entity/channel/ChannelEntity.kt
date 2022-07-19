package com.sceyt.chat.ui.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        var id: Long,
        var type: ChannelTypeEnum,
        var createdAt: Long = 0,
        var updatedAt: Long = 0,
        var unreadMessageCount: Long = 0,
        var lastMessageId: Long? = null,
        var lastMessageAt: Long? = null,
        var label: String?,
        var metadata: String?,
        var muted: Boolean = false,
        var muteExpireDate: Long?,
        var muteUntil: Long?,
        var subject: String?,
        var avatarUrl: String?,
        var memberCount: Long
)