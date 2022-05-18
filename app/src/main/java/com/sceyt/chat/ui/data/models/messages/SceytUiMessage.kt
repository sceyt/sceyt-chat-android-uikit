package com.sceyt.chat.ui.data.models.messages

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.*
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import java.util.*

open class SceytUiMessage(var id: Long,
                          var tid: Long,
                          var channelId: Long,
                          var to: String?,
                          var body: String,
                          var type: String,
                          var metadata: String,
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
                          var mentionedUsers: Array<User?>,
                          var parent: Message?,
                          var replyInThread: Boolean = false,
                          var replyCount: Long = 0) : BaseObservable() {

    @Bindable
    var status: DeliveryStatus = deliveryStatus
        get() = deliveryStatus
        set(value) {
            deliveryStatus = value
            field = value
            notifyPropertyChanged(BR.status)
        }

    @Bindable
    var showDate = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.showDate)
        }

    @Bindable
    var canShowAvatarAndName = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.canShowAvatarAndName)
        }

    var isGroup = false

    fun updateMessage(message: SceytUiMessage) {
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
        status = message.status
    }

    override fun equals(other: Any?): Boolean {
        return  (other is SceytUiMessage && other.id == id)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tid.hashCode()
        result = 31 * result + channelId.hashCode()
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + body.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + incoming.hashCode()
        result = 31 * result + receipt.hashCode()
        result = 31 * result + isTransient.hashCode()
        result = 31 * result + silent.hashCode()
        result = 31 * result + deliveryStatus.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (attachments?.contentHashCode() ?: 0)
        result = 31 * result + (lastReactions?.contentHashCode() ?: 0)
        result = 31 * result + (selfReactions?.contentHashCode() ?: 0)
        result = 31 * result + (reactionScores?.contentHashCode() ?: 0)
        result = 31 * result + (markerCount?.contentHashCode() ?: 0)
        result = 31 * result + (selfMarkers?.contentHashCode() ?: 0)
        result = 31 * result + mentionedUsers.contentHashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + replyInThread.hashCode()
        result = 31 * result + replyCount.hashCode()
        result = 31 * result + showDate.hashCode()
        result = 31 * result + canShowAvatarAndName.hashCode()
        result = 31 * result + isGroup.hashCode()
        return result
    }
}
