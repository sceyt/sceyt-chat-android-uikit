package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MENTION_USER_MESSAGE_LINK_TABLE

@Entity(
    tableName = MENTION_USER_MESSAGE_LINK_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ], primaryKeys = ["messageTid", "user_id"])
internal data class MentionUserMessageLinkEntity(
        @ColumnInfo(index = true)
        val messageTid: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String
)