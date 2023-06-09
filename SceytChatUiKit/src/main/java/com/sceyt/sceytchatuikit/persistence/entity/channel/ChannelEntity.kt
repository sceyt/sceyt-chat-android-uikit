package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        var id: Long,
        @ColumnInfo(index = true)
        val parentId: Long?,
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
        @ColumnInfo(index = true)
        var createdById: String?,
        @ColumnInfo(index = true)
        var role: String?,
        var unread: Boolean,
        var newMessageCount: Long,
        var newMentionCount: Long,
        var newReactionCount: Long,
        @ColumnInfo(index = true)
        var hidden:Boolean,
        @ColumnInfo(index = true)
        var archived: Boolean,
        var muted: Boolean,
        var mutedUntil: Long?,
        var pinnedAt: Long?,
        var lastReceivedMessageId: Long,
        var lastDisplayedMessageId: Long,
        var messageRetentionPeriod: Long,
        var lastMessageTid: Long?,
        @ColumnInfo(index = true)
        var lastMessageAt: Long?
)