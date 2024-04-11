package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.entity.pendings.PendingMessageStateDb
import com.sceyt.chatuikit.persistence.entity.pendings.PendingMessageStateEntity

@Dao
interface PendingMessageStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingMessageStateEntity)

    @Query("select * from PendingMessageState")
    suspend fun getAll(): List<PendingMessageStateEntity>

    @Transaction
    @Query("select * from PendingMessageState")
    suspend fun getAllWithMessage(): List<PendingMessageStateDb>

    @Query("delete from PendingMessageState where messageId =:messageId")
    suspend fun deleteByMessageId(messageId: Long)
}