package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.sceytchatuikit.persistence.entity.messages.LoadRangeEntity

@Dao
interface LoadRangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LoadRangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LoadRangeEntity>)

    @Query("select * from LoadRange where :messageId in (startId, endId)")
    suspend fun getLoadRange(messageId: Long): List<LoadRangeEntity>

    @Query("select * from LoadRange where (startId >=:start and startId <=:end)" +
            " or (endId >=:start and endId <= :end) or startId =:messageId or endId =:messageId")
    suspend fun getLoadRanges(start: Long, end: Long, messageId: Long): List<LoadRangeEntity>

    @Query("select * from LoadRange order by startId")
    suspend fun getAll(): List<LoadRangeEntity>

    @Query("delete from LoadRange where channelId =:channelId")
    suspend fun deleteChannelLoadRanges(channelId: Long)

    @Query("delete from LoadRange where rowId in (:rowIds)")
    suspend fun deleteLoadRanges(vararg rowIds: Long)

    @Transaction
    suspend fun updateLoadRanges(start: Long, end: Long, messageId: Long, channelId: Long) {
        val ranges = getLoadRanges(start, end, messageId)
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