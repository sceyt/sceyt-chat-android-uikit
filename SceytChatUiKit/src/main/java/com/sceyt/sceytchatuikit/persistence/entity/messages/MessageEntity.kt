package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerTotal
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
        var body: String,
        var type: String,
        var metadata: String?,
        @ColumnInfo(index = true)
        var createdAt: Long,
        var updatedAt: Long,
        var incoming: Boolean,
        var isTransient: Boolean,
        var silent: Boolean,
        @ColumnInfo(index = true)
        var deliveryStatus: DeliveryStatus,
        var state: MessageState,
        var fromId: String?,
        val markerCount: List<MarkerTotal>?,
        var mentionedUsersIds: List<String>?,
        var parentId: Long?,
        var replyCount: Long,
        val displayCount: Short,
        val autoDeleteAt: Long?,
        @Embedded
        val forwardingDetailsDb: ForwardingDetailsDb?,
        val bodyAttribute: List<BodyAttribute>?,
        @ColumnInfo(index = true)
        // This flag is used to ignore getting this message, when querying get channel messages
        var unList: Boolean
)