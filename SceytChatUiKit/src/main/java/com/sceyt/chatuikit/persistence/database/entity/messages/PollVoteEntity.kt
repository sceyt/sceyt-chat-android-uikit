package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_VOTE_TABLE

@Entity(
    tableName = POLL_VOTE_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PollEntity::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = PollOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["optionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index(value = ["pollId", "optionId", "userId"], unique = true),
        Index(value = ["optionId"]),
        Index(value = ["userId"])
    ]
)
internal data class PollVoteEntity(
        @PrimaryKey
        val id: String,
        val pollId: String,
        val optionId: String,
        val userId: String,
        val createdAt: Long,
)

