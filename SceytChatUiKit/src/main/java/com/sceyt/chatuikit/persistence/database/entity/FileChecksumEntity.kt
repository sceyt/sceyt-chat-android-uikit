package com.sceyt.chatuikit.persistence.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.FILE_CHECKSUM_TABLE

@Entity(tableName = FILE_CHECKSUM_TABLE)
internal data class FileChecksumEntity(
        @PrimaryKey
        val checksum: Long,
        val resizedFilePath: String?,
        val url: String?,
        val metadata: String?,
        val fileSize: Long?
)