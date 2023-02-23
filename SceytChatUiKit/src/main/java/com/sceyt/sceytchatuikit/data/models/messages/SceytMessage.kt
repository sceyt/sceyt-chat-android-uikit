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
                        var to: String?,
                        var body: String,
                        var type: String,
                        var metadata: String?,
                        var createdAt: Long,
                        var updatedAt: Date,
                        var incoming: Boolean,
                        var receipt: Boolean,
                        var isTransient: Boolean,
                        var silent: Boolean,
                        var direct: Boolean,
                        var deliveryStatus: DeliveryStatus,
                        var state: MessageState,
                        var from: User?,
                        var attachments: Array<SceytAttachment>?,
                        var selfReactions: Array<Reaction>? = null,
                        var reactionScores: Array<ReactionScore>?,
                        var markerCount: Array<MarkerCount>?,
                        var selfMarkers: Array<String>?,
                        var mentionedUsers: Array<User>? = null,
                        var parent: SceytMessage?,
                        var replyInThread: Boolean,
                        var replyCount: Long,
                        val displayCount: Short,
                        var forwardingDetails: ForwardingDetails?) : Parcelable, Cloneable {


    @IgnoredOnParcel
    var canShowAvatarAndName = false

    @IgnoredOnParcel
    var isGroup = false

    @IgnoredOnParcel
    var files: List<FileListItem>? = null

    @IgnoredOnParcel
    var messageReactions: List<ReactionItem>? = null

    val isForwarded get() = (forwardingDetails?.messageId ?: 0L) > 0L

    val isReplied get() = parent != null && parent?.id != 0L && !replyInThread

    fun updateMessage(message: SceytMessage) {
        id = message.id
        tid = message.tid
        channelId = message.channelId
        to = message.to
        body = message.body
        type = message.type
        metadata = message.metadata
        //createdAt = message.createdAt
        updatedAt = message.updatedAt
        incoming = message.incoming
        receipt = message.receipt
        isTransient = message.isTransient
        silent = message.silent
        deliveryStatus = message.deliveryStatus
        state = message.state
        from = message.from
        attachments = message.attachments
        selfReactions = message.selfReactions
        reactionScores = message.reactionScores
        markerCount = message.markerCount
        selfMarkers = message.selfMarkers
        mentionedUsers = message.mentionedUsers
        parent = message.parent
        replyInThread = message.replyInThread
        replyCount = message.replyCount
        reactionScores?.toMutableSet()?.retainAll {
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
            to = to,
            body = body,
            type = type,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            incoming = incoming,
            receipt = receipt,
            isTransient = isTransient,
            silent = silent,
            direct = direct,
            deliveryStatus = deliveryStatus,
            state = state,
            from = from,
            attachments = attachments?.map(SceytAttachment::clone)?.toTypedArray(),
            selfReactions = selfReactions,
            reactionScores = reactionScores,
            markerCount = markerCount,
            selfMarkers = selfMarkers,
            mentionedUsers = mentionedUsers,
            parent = parent,
            replyInThread = replyInThread,
            replyCount = replyCount,
            displayCount = displayCount,
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
