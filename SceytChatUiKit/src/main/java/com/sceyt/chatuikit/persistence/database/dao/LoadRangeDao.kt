package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOAD_RANGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity

@Dao
internal interface LoadRangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LoadRangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LoadRangeEntity>)

    @Query(
        """
        SELECT *
        FROM $LOAD_RANGE_TABLE
        WHERE channelId = :channelId
          AND ((startId >= :end AND endId <= :start)
           OR (endId >= :start AND startId <= :end)
           OR startId = :messageId
           OR endId = :messageId)
        """
    )
    suspend fun getLoadRanges(start: Long, end: Long, messageId: Long, channelId: Long): List<LoadRangeEntity>

    @Query("SELECT * FROM $LOAD_RANGE_TABLE WHERE channelId = :channelId ORDER BY startId")
    suspend fun getAll(channelId: Long): List<LoadRangeEntity>

    @Query("DELETE FROM $LOAD_RANGE_TABLE WHERE channelId = :channelId")
    suspend fun deleteChannelLoadRanges(channelId: Long)

    @Query("DELETE FROM $LOAD_RANGE_TABLE WHERE channelId IN (:channelIds)")
    suspend fun deleteChannelsLoadRanges(channelIds: List<Long>)

    @Query("DELETE FROM $LOAD_RANGE_TABLE WHERE rowId IN (:rowIds)")
    suspend fun deleteLoadRanges(vararg rowIds: Long)

    @Transaction
    suspend fun updateLoadRanges(start: Long, end: Long, messageId: Long, channelId: Long) {
        val ranges = getLoadRanges(start, end, messageId, channelId)
        val minDb = ranges.minByOrNull { it.startId }?.startId ?: start
        val maxDb = ranges.maxByOrNull { it.endId }?.endId ?: end
        val min = minOf(minDb, start)
        val max = maxOf(maxDb, end)

        if (ranges.size == 1 && min >= ranges[0].startId && max <= ranges[0].endId)
            return

        if (ranges.isNotEmpty())
            deleteLoadRanges(*ranges.map { it.rowId }.toLongArray())

        insert(LoadRangeEntity(min, max, channelId))
    }
}