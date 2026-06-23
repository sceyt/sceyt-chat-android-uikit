package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Entity
import androidx.room.Index
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOCAL_UNREAD_MESSAGE_TABLE

@Entity(
    tableName = LOCAL_UNREAD_MESSAGE_TABLE,
    primaryKeys = ["channelId", "messageId"],
    indices = [Index(value = ["channelId"])]
)
internal data class LocalUnreadMessageEntity(
    val channelId: Long,
    val messageId: Long,
    val mentioned: Boolean,
    val createdAt: Long,
)
