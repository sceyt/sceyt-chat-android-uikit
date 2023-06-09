package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerCount
import com.sceyt.chat.models.message.MessageState

@Entity(tableName = "messages", indices = [Index(value = ["message_id"], unique = true),
    Index(value = ["channelId"]), Index(value = ["createdAt"]),
    Index(value = ["deliveryStatus"]), Index(value = ["state"])])
data class MessageEntity(
        @PrimaryKey
        var tid: Long,
        @ColumnInfo(name = "message_id")
        var id: Long?,
        var channelId: Long,
        var to: String?,
        var body: String,
        var type: String,
        var metadata: String?,
        var createdAt: Long,
        var updatedAt: Long,
        var incoming: Boolean,
        var receipt: Boolean,
        var isTransient: Boolean,
        var silent: Boolean,
        var direct: Boolean,
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