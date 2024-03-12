package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "chat_id")
        val id: Long,
        val parentChannelId: Long?,
        val uri: String?,
        @ColumnInfo(index = true)
        val type: String,
        @ColumnInfo(index = true)
        val subject: String?,
        val avatarUrl: String?,
        val metadata: String?,
        @ColumnInfo(index = true)
        val createdAt: Long,
        val updatedAt: Long,
        val messagesClearedAt: Long,
        val memberCount: Long,
        val createdById: String?,
        @ColumnInfo(index = true)
        val userRole: String?,
        val unread: Boolean,
        val newMessageCount: Long,
        val newMentionCount: Long,
        val newReactedMessageCount: Long,
        val hidden: Boolean,
        val archived: Boolean,
        val muted: Boolean,
        val mutedTill: Long?,
        val pinnedAt: Long?,
        val lastReceivedMessageId: Long,
        val lastDisplayedMessageId: Long,
        val messageRetentionPeriod: Long,
        val lastMessageTid: Long?,
        @ColumnInfo(index = true)
        val lastMessageAt: Long?,
        @ColumnInfo(index = true)
        val pending: Boolean,
        @ColumnInfo(index = true, defaultValue = "false")
        val isSelf: Boolean
)