package com.sceyt.sceytchatuikit.persistence.entity.messages

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
        var id: Int = 0,
        @ColumnInfo(index = true)
        var messageTid: Long,
        @ColumnInfo(name = "user_id", index = true)
        var userId: String
)