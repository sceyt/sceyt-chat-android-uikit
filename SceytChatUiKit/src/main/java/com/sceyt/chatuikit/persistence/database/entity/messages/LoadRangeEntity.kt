package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOAD_RANGE_TABLE

@Entity(
    tableName = LOAD_RANGE_TABLE,
    indices = [
        Index(value = ["startId", "channelId"], unique = true),
        Index(value = ["endId", "channelId"], unique = true)
    ])
internal data class LoadRangeEntity(
        @ColumnInfo(index = true)
        val startId: Long,
        @ColumnInfo(index = true)
        val endId: Long,
        @ColumnInfo(index = true)
        val channelId: Long,
        @PrimaryKey(autoGenerate = true)
        val rowId: Long = 0
)