package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LINK_DETAILS_TABLE
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.mappers.mergeWith

@Dao
internal interface LinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LinkDetailsEntity)

    @Query("UPDATE $LINK_DETAILS_TABLE SET imageWidth = :imageWidth, imageHeight = :imageHeight WHERE link = :link")
    suspend fun updateSizes(link: String, imageWidth: Int, imageHeight: Int)

    @Query("UPDATE $LINK_DETAILS_TABLE SET thumb = :thumb WHERE link = :link")
    suspend fun updateThumb(link: String, thumb: String)

    @Transaction
    suspend fun upsert(entity: LinkDetailsEntity): LinkDetailsEntity {
        val old = getLinkDetailsEntity(entity.link)
        val merged = old?.mergeWith(entity) ?: entity
        insert(merged)
        return merged
    }

    @Query("SELECT * FROM $LINK_DETAILS_TABLE WHERE link = :link")
    suspend fun getLinkDetailsEntity(link: String): LinkDetailsEntity?
}