package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.sceyt.sceytchatuikit.persistence.entity.link.LinkDetailsEntity

@Dao
interface LinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LinkDetailsEntity)
}