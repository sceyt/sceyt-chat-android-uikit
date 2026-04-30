package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_SYNC_STATE_TABLE

@Entity(tableName = CHANNEL_SYNC_STATE_TABLE)
internal data class ChannelSyncStateEntity(
    @PrimaryKey
    val channelId: Long,
    val lastSyncedMessageId: Long
)
