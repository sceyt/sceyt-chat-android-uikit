package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOCAL_CHANNEL_UNREAD_STATE_TABLE

@Entity(tableName = LOCAL_CHANNEL_UNREAD_STATE_TABLE)
internal data class LocalChannelUnreadStateEntity(
    @PrimaryKey
    val channelId: Long,
    val baseUnreadCount: Long,
    val baseMentionCount: Long,
    val baseUntilMessageId: Long,
    val lastLocalReadMessageId: Long,
    val markedUnread: Boolean,
    val locallyManaged: Boolean,
    val updatedAt: Long,
)
