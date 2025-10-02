package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MESSAGE_STATE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateEntity

@Dao
internal interface PendingMessageStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingMessageStateEntity)

    @Query("select * from $PENDING_MESSAGE_STATE_TABLE")
    suspend fun getAll(): List<PendingMessageStateEntity>

    @Transaction
    @Query("select * from $PENDING_MESSAGE_STATE_TABLE")
    suspend fun getAllWithMessage(): List<PendingMessageStateDb>

    @Query("delete from $PENDING_MESSAGE_STATE_TABLE where messageId =:messageId")
    suspend fun deleteByMessageId(messageId: Long)
}