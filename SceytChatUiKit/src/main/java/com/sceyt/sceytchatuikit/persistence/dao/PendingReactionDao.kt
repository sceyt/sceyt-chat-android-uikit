package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.sceytchatuikit.persistence.entity.messages.PendingReactionEntity

@Dao
interface PendingReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingReactionEntity): Long

    @Query("select * from pendingReaction")
    suspend fun getAllData(): List<PendingReactionEntity>

    @Query("delete from pendingReaction where messageId =:messageId and reaction_key =:key")
    suspend fun deletePendingReaction(messageId: Long, key: String)
}