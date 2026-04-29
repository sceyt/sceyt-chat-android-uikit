package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LINK_DETAILS_TABLE
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity

@Dao
internal interface LinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LinkDetailsEntity)

    @Query(
        """
        UPDATE $LINK_DETAILS_TABLE
        SET url = :url, description = :desc, siteName = :siteName,
            faviconUrl = :favicon, imageUrl = :image
        WHERE link = :link
        """
    )
    suspend fun update(link: String, url: String?, desc: String?, siteName: String?, favicon: String?, image: String?)

    @Query("UPDATE $LINK_DETAILS_TABLE SET imageWidth = :imageWidth, imageHeight = :imageHeight WHERE link = :link")
    suspend fun updateSizes(link: String, imageWidth: Int, imageHeight: Int)

    @Query("UPDATE $LINK_DETAILS_TABLE SET thumb = :thumb WHERE link = :link")
    suspend fun updateThumb(link: String, thumb: String)

    @Transaction
    suspend fun upsert(entity: LinkDetailsEntity) {
        val old = getLinkDetailsEntity(entity.link)
        if (old == null)
            insert(entity)
        else {
            // If the old entity has an image, we don't want to replace it with a null image
            if (entity.imageWidth != null && entity.imageWidth > 0 || old.imageWidth == null)
                insert(entity)
            else update(entity.link, entity.url, entity.description, entity.siteName,
                entity.faviconUrl, entity.imageUrl)
        }
    }

    @Query("SELECT * FROM $LINK_DETAILS_TABLE WHERE link = :link")
    suspend fun getLinkDetailsEntity(link: String): LinkDetailsEntity?
}