package com.sceyt.sceytchatuikit.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FileChecksum")
data class FileChecksumEntity(
        @PrimaryKey
        val checksum: Long,
        val resizedFilePath: String?,
        val url: String?
)