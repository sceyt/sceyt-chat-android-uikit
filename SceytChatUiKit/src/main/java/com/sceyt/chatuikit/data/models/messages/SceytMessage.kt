package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMessage(
        val id: Long,
        val tid: Long,
        val channelId: Long,
        val body: String,
        val type: String,
        val metadata: String?,
        val createdAt: Long,
        val updatedAt: Long,
        val incoming: Boolean,
        val isTransient: Boolean,
        val silent: Boolean,
        val deliveryStatus: DeliveryStatus,
        val state: MessageState,
        val user: User?,
        val attachments: List<SceytAttachment>?,
        val userReactions: List<SceytReaction>?,
        val reactionTotals: List<ReactionTotal>?,
        val markerTotals: List<MarkerTotal>?,
        val userMarkers: List<SceytMarker>?,
        val mentionedUsers: List<User>?,
        val parentMessage: SceytMessage?,
        val replyCount: Long,
        val displayCount: Short,
        val autoDeleteAt: Long?,
        val forwardingDetails: ForwardingDetails?,
        val pendingReactions: List<PendingReactionData>?,
        val bodyAttributes: List<BodyAttribute>?,
    // Local properties
        val shouldShowAvatar: Boolean = false,
        val shouldShowName: Boolean = false,
        val disabledShowAvatarAndName: Boolean = false,
        val isGroup: Boolean = false,
        val files: List<FileListItem>? = null,
        val messageReactions: List<ReactionItem.Reaction>? = null,
        val isSelected: Boolean = false
) : Parcelable, Cloneable {

    val isForwarded get() = (forwardingDetails?.messageId ?: 0L) > 0L

    // todo reply in thread
    val isReplied get() = parentMessage != null && parentMessage.id != 0L /*&& !replyInThread*/

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
