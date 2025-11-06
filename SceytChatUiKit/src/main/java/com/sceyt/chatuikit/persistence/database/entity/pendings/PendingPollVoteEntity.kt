package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_POLL_VOTE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

@Entity(
    tableName = PENDING_POLL_VOTE_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index(value = ["pollId", "optionId", "userId"], unique = true),
        Index(value = ["optionId"]),
        Index(value = ["userId"]),
        Index(value = ["pollId"]),
        Index(value = ["messageTid"])
    ]
)
internal data class PendingPollVoteEntity(
        val messageTid: Long,
        val pollId: String,
        val optionId: String,
        val userId: String,
        val isAdd: Boolean, // true = add vote, false = remove vote
        val createdAt: Long,
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
)

