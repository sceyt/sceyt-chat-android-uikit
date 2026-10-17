package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_CHANNEL_AVATAR_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity

@Entity(
    tableName = PENDING_CHANNEL_AVATAR_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["chat_id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ])
internal data class PendingChannelAvatarEntity(
    @PrimaryKey
    val channelId: Long,
    val filePath: String,
)
