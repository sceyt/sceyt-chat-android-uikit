package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerWithUserDb

@Dao
interface MarkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMany(marker: List<MarkerEntity>)

    @Transaction
    @Query("select * from MarkerEntity where messageId =:messageId and name in (:names) " +
            "order by createdAt desc limit :limit offset :offset")
    suspend fun getMessageMarkers(messageId: Long, names: List<String>, offset: Int, limit: Int): List<MarkerWithUserDb>
}