package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.*

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
    indices = [
        Index(value = ["messageTid"]),
        Index(value = ["messageTid", "url"], unique = true)])
data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        val id: Long?,
        val messageId: Long,
        val messageTid: Long,
        var userId: String?,
        val tid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        val createdAt: Long,
        @ColumnInfo(index = true)
        val url: String?,
        val filePath: String?
)