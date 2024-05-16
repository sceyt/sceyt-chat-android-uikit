package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.entity.pendings.PendingMarkerEntity

@Dao
interface PendingMarkerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingMarkerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(entities: List<PendingMarkerEntity>)

    @Query("select * from PendingMarker")
    suspend fun getAllMarkers(): List<PendingMarkerEntity>

    @Query("delete from PendingMarker where messageId =:messageId and name =:status")
    suspend fun deleteMessageMarkerByStatus(messageId: Long, status: String)

    @Query("delete from PendingMarker where messageId in (:messageIds) and name =:status")
    suspend fun deleteMessagesMarkersByStatus(messageIds: List<Long>, status: String)
}