package com.sceyt.chat.ui.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState

@Entity(tableName = "messages")
data class MessageEntity(
        @PrimaryKey
        @ColumnInfo(name = "message_id")
        var id: Long,
        var tid: Long,
        var channelId: Long,
        var to: String?,
        var body: String,
        var type: String,
        var metadata: String? = null,
        var createdAt: Long,
        var updatedAt: Long,
        var incoming: Boolean = false,
        var receipt: Boolean = false,
        var isTransient: Boolean = false,
        var silent: Boolean = false,
        var deliveryStatus: DeliveryStatus,
        var state: MessageState,
        var fromId: String?,
       /* var attachments: Array<Attachment>? = null,
        var lastReactions: Array<Reaction>? = null,
        var selfReactions: Array<Reaction>? = null,
        var reactionScores: Array<ReactionScore>? = null,
        var markerCount: Array<MarkerCount>? = null,
        var selfMarkers: Array<String>? = null,
        var mentionedUsers: Array<User>?,*/
        var parentId: Long?,
        var replyInThread: Boolean = false,
        var replyCount: Long = 0
)