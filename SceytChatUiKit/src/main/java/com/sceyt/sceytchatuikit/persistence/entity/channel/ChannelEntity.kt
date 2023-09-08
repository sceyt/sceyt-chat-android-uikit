package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        var id: Long,
        val parentChannelId: Long?,
        var uri: String?,
        @ColumnInfo(index = true)
        var type: String,
        @ColumnInfo(index = true)
        var subject: String?,
        var avatarUrl: String?,
        var metadata: String?,
        @ColumnInfo(index = true)
        var createdAt: Long,
        var updatedAt: Long,
        var messagesClearedAt: Long,
        var memberCount: Long,
        var createdById: String?,
        @ColumnInfo(index = true)
        var userRole: String?,
        var unread: Boolean,
        var newMessageCount: Long,
        var newMentionCount: Long,
        var newReactedMessageCount: Long,
        var hidden: Boolean,
        var archived: Boolean,
        var muted: Boolean,
        var mutedTill: Long?,
        var pinnedAt: Long?,
        var lastReceivedMessageId: Long,
        var lastDisplayedMessageId: Long,
        var messageRetentionPeriod: Long,
        var lastMessageTid: Long?,
        @ColumnInfo(index = true)
        var lastMessageAt: Long?,
        @ColumnInfo(index = true)
        var pending: Boolean
)