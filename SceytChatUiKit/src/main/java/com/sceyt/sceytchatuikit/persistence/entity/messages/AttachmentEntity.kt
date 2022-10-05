package com.sceyt.sceytchatuikit.persistence.entity.messages

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
    indices = [
        Index(value = ["messageTid"]),
        Index(value = ["messageTid", "url"], unique = true)])
data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val messageId: Long,
        val messageTid: Long,
        val tid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        val url: String
)