package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerWithUserDb

@Dao
interface MarkerDao {

    @Transaction
    @Query("select * from MarkerEntity where messageId =:messageId and name in (:names) " +
            "order by createdAt desc limit :limit offset :offset")
    suspend fun getMessageMarkers(messageId: Long, names: List<String>, offset: Int, limit: Int): List<MarkerWithUserDb>
}