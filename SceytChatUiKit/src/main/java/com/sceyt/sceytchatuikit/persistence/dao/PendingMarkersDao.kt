package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.PendingMarkersEntity

@Dao
interface PendingMarkersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingMarkersEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(entities: List<PendingMarkersEntity>)

    @Query("select * from PendingMarkers where messageId =:messageId and name =:status")
    suspend fun getMarkersByStatus(messageId: Long, status: MarkerTypeEnum): List<PendingMarkersEntity>


    @Query("select * from PendingMarkers where name =:status")
    suspend fun getAllMarkersByStatus(status: MarkerTypeEnum): List<PendingMarkersEntity>

    @Query("select * from PendingMarkers")
    suspend fun getAllMarkers(): List<PendingMarkersEntity>

    @Query("delete from PendingMarkers where messageId =:messageId and name =:status")
    suspend fun deleteMessageMarkerByStatus(messageId: Long, status: MarkerTypeEnum)

    @Query("delete from PendingMarkers where messageId in (:messageIds) and name =:status")
    suspend fun deleteMessagesMarkersByStatus(messageIds: List<Long>, status: MarkerTypeEnum)
}