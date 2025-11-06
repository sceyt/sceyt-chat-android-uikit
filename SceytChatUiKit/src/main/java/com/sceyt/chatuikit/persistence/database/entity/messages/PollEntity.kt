package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_TABLE

@Entity(
    tableName = POLL_TABLE,
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
        Index(value = ["messageTid"], unique = true),
        Index(value = ["pollId"])
    ]
)
internal data class PollEntity(
    val pollId: String,
    val messageTid: Long,
    val name: String,
    val description: String,
    val anonymous: Boolean,
    val votesPerOption: Map<String, Int>,
    val allowMultipleVotes: Boolean,
    val allowVoteRetract: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long,
    val closed: Boolean,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

