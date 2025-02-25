package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.entity.messages.LOAD_RANGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity

@Dao
internal interface LoadRangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LoadRangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LoadRangeEntity>)

    @Query("select * from $LOAD_RANGE_TABLE where channelId =:channelId and ((startId >=:end and endId <=:start)" +
            " or (endId >=:start and startId <= :end) or startId =:messageId or endId =:messageId)")
    suspend fun getLoadRanges(start: Long, end: Long, messageId: Long, channelId: Long): List<LoadRangeEntity>

    @Query("select * from $LOAD_RANGE_TABLE where channelId =:channelId order by startId")
    suspend fun getAll(channelId: Long): List<LoadRangeEntity>

    @Query("delete from $LOAD_RANGE_TABLE where channelId =:channelId")
    suspend fun deleteChannelLoadRanges(channelId: Long)

    @Query("delete from $LOAD_RANGE_TABLE where channelId in (:channelIds)")
    suspend fun deleteChannelsLoadRanges(channelIds: List<Long>)

    @Query("delete from $LOAD_RANGE_TABLE where rowId in (:rowIds)")
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