package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.*
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
open class SceytMessage(var id: Long,
                        var tid: Long,
                        var channelId: Long,
                        var body: String,
                        var type: String,
                        var metadata: String?,
                        var createdAt: Long,
                        var updatedAt: Date,
                        var incoming: Boolean,
                        var isTransient: Boolean,
                        var silent: Boolean,
                        var deliveryStatus: DeliveryStatus,
                        var state: MessageState,
                        var user: User?,
                        var attachments: Array<SceytAttachment>?,
                        var userReactions: Array<Reaction>?,
                        var reactionTotals: Array<ReactionTotal>?,
                        var markerTotals: Array<MarkerTotal>?,
                        var userMarkers: Array<String>?,
                        var mentionedUsers: Array<User>?,
                        var parentMessage: SceytMessage?,
                        var replyCount: Long,
                        val displayCount: Short,
                        var autoDeleteDate: Long?,
                        var forwardingDetails: ForwardingDetails?) : Parcelable, Cloneable {


    @IgnoredOnParcel
    var canShowAvatarAndName = false

    @IgnoredOnParcel
    var isGroup = false

    @IgnoredOnParcel
    var files: List<FileListItem>? = null

    @IgnoredOnParcel
    var messageReactions: List<ReactionItem.Reaction>? = null

    val isForwarded get() = (forwardingDetails?.messageId ?: 0L) > 0L

    // todo reply in thread
    val isReplied get() = parentMessage != null && parentMessage?.id != 0L /*&& !replyInThread*/

    fun updateMessage(message: SceytMessage) {
        id = message.id
        tid = message.tid
        channelId = message.channelId
        body = message.body
        type = message.type
        metadata = message.metadata
        //createdAt = message.createdAt
        updatedAt = message.updatedAt
        incoming = message.incoming
        isTransient = message.isTransient
        silent = message.silent
        deliveryStatus = message.deliveryStatus
        state = message.state
        user = message.user
        attachments = message.attachments
        userReactions = message.userReactions
        reactionTotals = message.reactionTotals
        markerTotals = message.markerTotals
        userMarkers = message.userMarkers
        mentionedUsers = message.mentionedUsers
        parentMessage = message.parentMessage
        replyCount = message.replyCount
        autoDeleteDate = message.autoDeleteDate
        reactionTotals?.toMutableSet()?.retainAll {
            it.key == ""
        }
        // Update inner data
        messageReactions = message.messageReactions
        files = message.files
    }

    public override fun clone(): SceytMessage {
        return SceytMessage(
            id = id,
            tid = tid,
            channelId = channelId,
            body = body,
            type = type,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            incoming = incoming,
            isTransient = isTransient,
            silent = silent,
            deliveryStatus = deliveryStatus,
            state = state,
            user = user,
            attachments = attachments?.map(SceytAttachment::clone)?.toTypedArray(),
            userReactions = userReactions,
            reactionTotals = reactionTotals,
            markerTotals = markerTotals,
            userMarkers = userMarkers,
            mentionedUsers = mentionedUsers,
            parentMessage = parentMessage,
            replyCount = replyCount,
            displayCount = displayCount,
            autoDeleteDate = autoDeleteDate,
            forwardingDetails = forwardingDetails).also {
            it.messageReactions = messageReactions
            it.files = files
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytMessage) return false

        return if (deliveryStatus == DeliveryStatus.Pending || other.deliveryStatus == DeliveryStatus.Pending)
            other.tid == tid
        else other.id == id
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode()
    }
}
