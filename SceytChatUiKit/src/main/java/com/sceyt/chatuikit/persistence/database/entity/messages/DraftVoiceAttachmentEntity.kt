package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_VOICE_ATTACHMENT_TABLE

@Entity(tableName = DRAFT_VOICE_ATTACHMENT_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = DraftMessageEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ])
internal data class DraftVoiceAttachmentEntity(
        @ColumnInfo(index = true)
        val chatId: Long,
        val filePath: String,
        val duration: Int,
        val amplitudes: List<Int>,
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
)
