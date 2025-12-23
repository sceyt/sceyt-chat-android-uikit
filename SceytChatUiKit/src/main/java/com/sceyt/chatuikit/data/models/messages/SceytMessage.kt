package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
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
    val viewOnce: Boolean,
    val deliveryStatus: MessageDeliveryStatus,
    val state: MessageState,
    val user: SceytUser?,
    val attachments: List<SceytAttachment>?,
    val userReactions: List<SceytReaction>?,
    val reactionTotals: List<ReactionTotal>?,
    val markerTotals: List<MarkerTotal>?,
    val userMarkers: List<SceytMarker>?,
    val mentionedUsers: List<SceytUser>?,
    val parentMessage: SceytMessage?,
    val replyCount: Long,
    val displayCount: Short,
    val autoDeleteAt: Long?,
    val forwardingDetails: ForwardingDetails?,
    val pendingReactions: List<PendingReactionData>?,
    val bodyAttributes: List<BodyAttribute>?,
    val disableMentionsCount: Boolean,
    val poll: SceytPollDetails?,
    // Local properties
    val shouldShowAvatarAndName: Boolean = false,
    val disabledShowAvatarAndName: Boolean = false,
    val isGroup: Boolean = false,
    val files: List<FileListItem>? = null,
    val messageReactions: List<ReactionItem.Reaction>? = null,
    val isSelected: Boolean = false,
    val isBodyExpanded: Boolean = false,
) : Parcelable, Cloneable {

    val isForwarded get() = (forwardingDetails?.messageId ?: 0L) > 0L

    // todo reply in thread
    val isReplied get() = parentMessage != null && parentMessage.id != 0L /*&& !replyInThread*/

    override fun equals(other: Any?): Boolean {
        return other is SceytMessage && other.tid == tid
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode()
    }
}
