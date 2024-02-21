package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "LoadRange",
    indices = [
        Index(value = ["startId"], unique = true),
        Index(value = ["endId"], unique = true),
        Index(value = ["channelId"])
    ])
data class LoadRangeEntity(
        val startId: Long,
        val endId: Long,
        val channelId: Long,
        @PrimaryKey(autoGenerate = true)
        val rowId: Long = 0
)