package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [Index(value = ["messageTid", "url"], unique = true)]
)
data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        @ColumnInfo(index = true)
        val id: Long?,
        val messageId: Long,
        @ColumnInfo(index = true)
        val messageTid: Long,
        val channelId: Long,
        var userId: String?,
        val name: String,
        @ColumnInfo(index = true)
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        @ColumnInfo(index = true)
        val createdAt: Long,
        @ColumnInfo(index = true)
        val url: String?,
        val filePath: String?,
        val originalFilePath: String?
)