package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.sceytchatuikit.persistence.entity.FileChecksumEntity

@Dao
interface FileChecksumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fileChecksum: FileChecksumEntity)

    @Query("select * from FileChecksum where checksum = :checksum")
    suspend fun getChecksum(checksum: Long): FileChecksumEntity?

    @Query("update FileChecksum set url = :url where checksum = :checksum")
    suspend fun updateUrl(checksum: Long, url: String?)

    @Query("update FileChecksum set resizedFilePath = :path, fileSize =:fileSize where checksum = :checksum")
    suspend fun updateResizedFilePathAndSize(checksum: Long, path: String?, fileSize: Long?)
}