package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE

@Entity(
    tableName = MESSAGE_TABLE,
    indices = [Index(value = ["message_id"], unique = true)]
)
internal data class MessageEntity(
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
        val markerCount: List<MarkerTotal>?,
        val mentionedUsersIds: List<String>?,
        val parentId: Long?,
        val replyCount: Long,
        val displayCount: Short,
        val autoDeleteAt: Long?,
        @Embedded
        val forwardingDetailsDb: ForwardingDetailsDb?,
        val bodyAttribute: List<BodyAttribute>?,
        @ColumnInfo(index = true)
        // This flag is used to ignore getting this message, when querying get channel messages
        val unList: Boolean
)