package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MARKER_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerWithUserDb

@Dao
internal interface MarkerDao {

    @Transaction
    @Query(
        """
        SELECT *
        FROM $MARKER_TABLE
        WHERE messageId = :messageId
          AND name IN (:names)
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMessageMarkers(
        messageId: Long,
        names: List<String>,
        offset: Int,
        limit: Int
    ): List<MarkerWithUserDb>
}