package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "LoadRange",
    indices = [
        Index(value = ["startId", "channelId"], unique = true),
        Index(value = ["endId", "channelId"], unique = true)
    ])
data class LoadRangeEntity(
        @ColumnInfo(index = true)
        val startId: Long,
        @ColumnInfo(index = true)
        val endId: Long,
        @ColumnInfo(index = true)
        val channelId: Long,
        @PrimaryKey(autoGenerate = true)
        val rowId: Long = 0
)