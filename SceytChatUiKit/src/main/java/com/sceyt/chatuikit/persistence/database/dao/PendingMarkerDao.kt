package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.entity.pendings.PENDING_MARKER_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMarkerEntity

@Dao
interface PendingMarkerDao {

    @Query("select * from $PENDING_MARKER_TABLE")
    suspend fun getAllMarkers(): List<PendingMarkerEntity>

    @Query("delete from $PENDING_MARKER_TABLE where messageId in (:messageIds) and name =:status")
    suspend fun deleteMessagesMarkersByStatus(messageIds: List<Long>, status: String)
}