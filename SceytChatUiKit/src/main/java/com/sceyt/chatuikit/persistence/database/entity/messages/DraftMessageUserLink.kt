package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["chatId", "user_id"],
    foreignKeys = [ForeignKey(
        entity = DraftMessageEntity::class,
        parentColumns = ["chatId"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )])
data class DraftMessageUserLink(
        val chatId: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String)