package com.sceyt.chat.ui.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.*
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
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
                        var metadata: String? = null,
                        var createdAt: Long,
                        var updatedAt: Date,
                        var incoming: Boolean = false,
                        var receipt: Boolean = false,
                        var isTransient: Boolean = false,
                        var silent: Boolean = false,
                        var deliveryStatus: DeliveryStatus,
                        var state: MessageState,
                        var from: User?,
                        var attachments: Array<Attachment>? = null,
                        var lastReactions: Array<Reaction>? = null,
                        var selfReactions: Array<Reaction>? = null,
                        var reactionScores: Array<ReactionScore>? = null,
                        var markerCount: Array<MarkerCount>? = null,
                        var selfMarkers: Array<String>? = null,
                        var mentionedUsers: Array<User>? = null,
                        var parent: Message?,
                        var replyInThread: Boolean = false,
                        var replyCount: Long = 0) : Parcelable, Cloneable {


    @IgnoredOnParcel
    var canShowAvatarAndName = false

    @IgnoredOnParcel
    var isGroup = false

    @IgnoredOnParcel
    var files: List<FileListItem>? = null

    @IgnoredOnParcel
    var messageReactions: List<ReactionItem>? = null

    fun updateMessage(message: SceytMessage) {
        id = message.id
        tid = message.tid
        channelId = message.channelId
        to = message.to
        body = message.body
        type = message.type
        metadata = message.metadata
        createdAt = message.createdAt
        updatedAt = message.updatedAt
        incoming = message.incoming
        receipt = message.receipt
        isTransient = message.isTransient
        silent = message.silent
        deliveryStatus = message.deliveryStatus
        state = message.state
        from = message.from
        attachments = message.attachments
        lastReactions = message.lastReactions
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
    }

    public override fun clone(): SceytMessage {
        return SceytMessage(id, tid, channelId, to, body, type,
            metadata, createdAt, updatedAt, incoming, receipt, isTransient, silent, deliveryStatus,
            state, from, attachments, lastReactions, selfReactions, reactionScores, markerCount,
            selfMarkers, mentionedUsers, parent)
    }
}
