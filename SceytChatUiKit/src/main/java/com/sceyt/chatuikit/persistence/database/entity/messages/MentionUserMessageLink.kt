package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["tid"],
        childColumns = ["messageTid"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
], primaryKeys = ["messageTid", "user_id"])
data class MentionUserMessageLink(
        @ColumnInfo(index = true)
        val messageTid: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String
)