package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.entity.messages.MarkerWithUserDb

@Dao
interface MarkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMarkers(userMarker: List<MarkerEntity>)

    @Query("select * from MarkerEntity where messageId =:messageId and name in (:names) " +
            "order by createdAt desc limit :limit offset :offset")
    suspend fun getMessageMarkers(messageId: Long, names: List<String>, offset: Int, limit: Int): List<MarkerWithUserDb>
}