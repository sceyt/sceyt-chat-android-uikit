package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity

internal data class PollDb(
        @Embedded
        val pollEntity: PollEntity,

        @Relation(parentColumn = "id", entityColumn = "pollId")
        val options: List<PollOptionEntity>,

        @Relation(
            parentColumn = "id",
            entityColumn = "pollId",
            entity = PollVoteEntity::class
        )
        val votes: List<PollVoteDb>?,

        @Relation(
            parentColumn = "id",
            entityColumn = "pollId",
            entity = PendingPollVoteEntity::class
        )
        val pendingVotes: List<PendingPollVoteDb>?,
)

