package com.sceyt.chat.ui.persistence.entity.messages

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [
    Index(value = ["messageId"]),
    Index(value = ["messageId", "url"], unique = true)])
data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val messageId: Long,
        val tid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        val url: String
)