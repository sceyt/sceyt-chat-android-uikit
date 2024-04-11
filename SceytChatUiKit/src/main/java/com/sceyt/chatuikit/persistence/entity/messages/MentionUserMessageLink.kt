package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.*

@Entity(foreignKeys = [
    ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["tid"],
        childColumns = ["messageTid"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
], indices = [Index(value = ["messageTid", "user_id"], unique = true, name = "uniqueMentionUserInMessage")])
data class MentionUserMessageLink(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        @ColumnInfo(index = true)
        val messageTid: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String
)