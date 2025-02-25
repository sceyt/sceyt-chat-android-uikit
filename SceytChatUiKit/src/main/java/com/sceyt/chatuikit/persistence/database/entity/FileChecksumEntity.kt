package com.sceyt.chatuikit.persistence.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

internal const val FILE_CHECKSUM_TABLE = "sceyt_file_checksum_table"

@Entity(tableName = FILE_CHECKSUM_TABLE)
internal data class FileChecksumEntity(
        @PrimaryKey
        val checksum: Long,
        val resizedFilePath: String?,
        val url: String?,
        val metadata: String?,
        val fileSize: Long?
)