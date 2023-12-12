package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class SceytMessage(var id: Long,
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
                        var userReactions: Array<SceytReaction>?,
                        var reactionTotals: Array<ReactionTotal>?,
                        var markerTotals: Array<MarkerTotal>?,
                        var userMarkers: Array<Marker>?,
                        var mentionedUsers: Array<User>?,
                        var parentMessage: SceytMessage?,
                        var replyCount: Long,
                        val displayCount: Short,
                        var autoDeleteAt: Long?,
                        var forwardingDetails: ForwardingDetails?,
                        var pendingReactions: List<PendingReactionData>?,
                        var bodyAttributes: List<BodyAttribute>?) : Parcelable, Cloneable {


    @IgnoredOnParcel
    var shouldShowAvatarAndName = false

    @IgnoredOnParcel
    var disabledShowAvatarAndName = false

    @IgnoredOnParcel
    var isGroup = false

    @IgnoredOnParcel
    var files: List<FileListItem>? = null

    @IgnoredOnParcel
    var messageReactions: List<ReactionItem.Reaction>? = null

    val isForwarded get() = (forwardingDetails?.messageId ?: 0L) > 0L

    // todo reply in thread
    val isReplied get() = parentMessage != null && parentMessage?.id != 0L /*&& !replyInThread*/

    @IgnoredOnParcel
    var isSelected: Boolean = false

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
        autoDeleteAt = message.autoDeleteAt
        reactionTotals?.toMutableSet()?.retainAll {
            it.key == ""
        }
        // Update inner data
        messageReactions = message.messageReactions
        files = message.files?.map { it.sceytMessage = this; it }
        pendingReactions = message.pendingReactions
        bodyAttributes = message.bodyAttributes
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
            parentMessage = parentMessage?.clone(),
            replyCount = replyCount,
            displayCount = displayCount,
            autoDeleteAt = autoDeleteAt,
            forwardingDetails = forwardingDetails,
            pendingReactions = pendingReactions,
            bodyAttributes = bodyAttributes).also {
            it.shouldShowAvatarAndName = shouldShowAvatarAndName
            it.disabledShowAvatarAndName = disabledShowAvatarAndName
            it.isGroup = isGroup
            it.files = files
            it.messageReactions = messageReactions
            it.isSelected = isSelected
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
