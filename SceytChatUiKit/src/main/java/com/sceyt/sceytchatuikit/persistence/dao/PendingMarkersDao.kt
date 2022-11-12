package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.PendingMarkersEntity

@Dao
interface PendingMarkersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingMarkersEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(entities: List<PendingMarkersEntity>)

    @Query("select * from PendingMarkers where messageId =:messageId and status =:status")
    suspend fun getMarkersByStatus(messageId: Long, status: SelfMarkerTypeEnum): List<PendingMarkersEntity>


    @Query("select * from PendingMarkers where status =:status")
    suspend fun getAllMarkersByStatus(status: SelfMarkerTypeEnum): List<PendingMarkersEntity>

    @Query("select * from PendingMarkers")
    suspend fun getAllMarkers(): List<PendingMarkersEntity>

    @Query("delete from PendingMarkers where messageId =:messageId and status =:status")
    suspend fun deleteMessageMarkerByStatus(messageId: Long, status: SelfMarkerTypeEnum)

    @Query("delete from PendingMarkers where messageId in (:messageIds) and status =:status")
    suspend fun deleteMessagesMarkersByStatus(messageIds: List<Long>, status: SelfMarkerTypeEnum)
}