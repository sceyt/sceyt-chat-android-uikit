package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MESSAGE_DELETE_BY_TID_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageDeleteByTidEntity

@Dao
internal interface PendingMessageDeleteByTidDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingMessageDeleteByTidEntity)

    @Query("SELECT * FROM $PENDING_MESSAGE_DELETE_BY_TID_TABLE ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingMessageDeleteByTidEntity>

    @Query("DELETE FROM $PENDING_MESSAGE_DELETE_BY_TID_TABLE WHERE messageTid = :tid")
    suspend fun deleteByTid(tid: Long)

    @Query("UPDATE $PENDING_MESSAGE_DELETE_BY_TID_TABLE SET channelId = :newChannelId WHERE channelId = :oldChannelId")
    suspend fun updateChannelId(oldChannelId: Long, newChannelId: Long): Int
}