package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.entity.pendings.PendingReactionEntity

@Dao
interface PendingReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingReactionEntity): Long

    @Query("select * from pendingReaction")
    suspend fun getAll(): List<PendingReactionEntity>

    @Query("select * from pendingReaction where channelId =:channelId")
    suspend fun getAllByChannelId(channelId: Long): List<PendingReactionEntity>

    @Query("select * from pendingReaction where messageId =:messageId")
    suspend fun getAllByMsgId(messageId: Long): List<PendingReactionEntity>

    @Query("select * from pendingReaction where messageId =:messageId and reaction_key =:key")
    suspend fun getAllByMsgIdAndKey(messageId: Long, key: String): List<PendingReactionEntity>

    @Query("delete from pendingReaction where messageId =:messageId and reaction_key =:key")
    suspend fun deletePendingReaction(messageId: Long, key: String)
}