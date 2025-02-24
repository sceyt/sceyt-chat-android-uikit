package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMarkerEntity

@Dao
interface PendingMarkerDao {

    @Query("select * from PendingMarker")
    suspend fun getAllMarkers(): List<PendingMarkerEntity>

    @Query("delete from PendingMarker where messageId =:messageId and name =:status")
    suspend fun deleteMessageMarkerByStatus(messageId: Long, status: String)

    @Query("delete from PendingMarker where messageId in (:messageIds) and name =:status")
    suspend fun deleteMessagesMarkersByStatus(messageIds: List<Long>, status: String)
}