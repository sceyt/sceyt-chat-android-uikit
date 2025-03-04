package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.entity.FILE_CHECKSUM_TABLE
import com.sceyt.chatuikit.persistence.database.entity.FileChecksumEntity

@Dao
internal interface FileChecksumDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fileChecksum: FileChecksumEntity)

    @Query("select * from $FILE_CHECKSUM_TABLE where checksum = :checksum")
    suspend fun getChecksum(checksum: Long): FileChecksumEntity?

    @Query("update $FILE_CHECKSUM_TABLE set url = :url where checksum = :checksum")
    suspend fun updateUrl(checksum: Long, url: String?)

    @Query("update $FILE_CHECKSUM_TABLE set resizedFilePath = :path, fileSize =:fileSize where checksum = :checksum")
    suspend fun updateResizedFilePathAndSize(checksum: Long, path: String?, fileSize: Long?)
}