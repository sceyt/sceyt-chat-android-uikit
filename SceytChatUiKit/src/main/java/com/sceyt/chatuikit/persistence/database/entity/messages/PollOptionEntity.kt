package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_OPTION_TABLE

@Entity(
    tableName = POLL_OPTION_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PollEntity::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [Index(value = ["pollId", "id"], unique = true)]
)
internal data class PollOptionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(index = true)
    val pollId: String,
    val name: String,
)

