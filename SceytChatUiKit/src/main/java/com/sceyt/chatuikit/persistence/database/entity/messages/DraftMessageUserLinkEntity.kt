package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_MESSAGE_USER_LINK_TABLE

@Entity(
    tableName = DRAFT_MESSAGE_USER_LINK_TABLE,
    primaryKeys = ["chatId", "user_id"],
    foreignKeys = [ForeignKey(
        entity = DraftMessageEntity::class,
        parentColumns = ["chatId"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
        deferred = true
    )])
internal data class DraftMessageUserLinkEntity(
        val chatId: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String)