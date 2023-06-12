package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerCount
import com.sceyt.chat.models.message.MessageState

@Entity(tableName = "messages",
    indices = [Index(value = ["message_id"], unique = true)])
data class MessageEntity(
        @PrimaryKey
        var tid: Long,
        @ColumnInfo(name = "message_id")
        var id: Long?,
        @ColumnInfo(index = true)
        var channelId: Long,
        var to: String?,
        var body: String,
        var type: String,
        var metadata: String?,
        @ColumnInfo(index = true)
        var createdAt: Long,
        var updatedAt: Long,
        var incoming: Boolean,
        var receipt: Boolean,
        var isTransient: Boolean,
        var silent: Boolean,
        var direct: Boolean,
        @ColumnInfo(index = true)
        var deliveryStatus: DeliveryStatus,
        var state: MessageState,
        var fromId: String?,
        var markerCount: List<MarkerCount>?,
        var mentionedUsersIds: List<String>?,
        var selfMarkers: List<String>?,
        var parentId: Long?,
        var replyInThread: Boolean,
        var replyCount: Long,
        val displayCount: Short,
        @Embedded
        val forwardingDetailsDb: ForwardingDetailsDb?,
        @ColumnInfo(index = true)
        var isParentMessage: Boolean
)