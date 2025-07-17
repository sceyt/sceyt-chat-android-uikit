package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE

@Entity(
    tableName = ATTACHMENT_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [Index(value = ["messageTid", "url"], unique = true)]
)
internal data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        @ColumnInfo(index = true)
        val id: Long?,
        val messageId: Long,
        @ColumnInfo(index = true)
        val messageTid: Long,
        val channelId: Long,
        val userId: String?,
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