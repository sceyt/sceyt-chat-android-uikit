package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MESSAGE_DELETE_BY_TID_TABLE

@Entity(tableName = PENDING_MESSAGE_DELETE_BY_TID_TABLE)
internal data class PendingMessageDeleteByTidEntity(
    @PrimaryKey
    val messageTid: Long,
    val channelId: Long,
    val deleteType: DeleteMessageType,
    val createdAt: Long,
)