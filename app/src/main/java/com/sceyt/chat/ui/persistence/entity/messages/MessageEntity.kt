package com.sceyt.chat.ui.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState

@Entity(tableName = "messages", indices = [Index(value = ["message_id"], unique = true)])
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
        var createdAt: Long,
        var updatedAt: Long,
        var incoming: Boolean,
        var receipt: Boolean,
        var isTransient: Boolean,
        var silent: Boolean,
        var deliveryStatus: DeliveryStatus,
        var state: MessageState,
        var fromId: String?,
    /*
     var lastReactions: Array<Reaction>? = null,
     var selfReactions: Array<Reaction>? = null,
     var reactionScores: Array<ReactionScore>? = null,
     var markerCount: Array<MarkerCount>? = null,
     var selfMarkers: Array<String>? = null,
     var mentionedUsers: Array<User>?,*/
        var parentId: Long?,
        var replyInThread: Boolean,
        var replyCount: Long
)