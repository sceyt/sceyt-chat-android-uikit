package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.SceytBodyAttribute
import com.sceyt.chatuikit.data.models.messages.SceytMarkerTotal

@Entity(tableName = "messages",
    indices = [Index(value = ["message_id"], unique = true)])
data class MessageEntity(
        @PrimaryKey
        val tid: Long,
        @ColumnInfo(name = "message_id")
        val id: Long?,
        @ColumnInfo(index = true)
        val channelId: Long,
        val body: String,
        val type: String,
        val metadata: String?,
        @ColumnInfo(index = true)
        val createdAt: Long,
        val updatedAt: Long,
        val incoming: Boolean,
        val isTransient: Boolean,
        val silent: Boolean,
        @ColumnInfo(index = true)
        val deliveryStatus: DeliveryStatus,
        val state: MessageState,
        val fromId: String?,
        val markerCount: List<SceytMarkerTotal>?,
        val mentionedUsersIds: List<String>?,
        val parentId: Long?,
        val replyCount: Long,
        val displayCount: Short,
        val autoDeleteAt: Long?,
        @Embedded
        val forwardingDetailsDb: ForwardingDetailsDb?,
        val bodyAttribute: List<SceytBodyAttribute>?,
        @ColumnInfo(index = true)
        // This flag is used to ignore getting this message, when querying get channel messages
        val unList: Boolean
)