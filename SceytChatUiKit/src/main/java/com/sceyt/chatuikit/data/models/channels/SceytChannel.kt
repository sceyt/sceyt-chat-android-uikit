package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.messages.PendingReactionData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isGroup
import com.sceyt.chatuikit.persistence.extensions.isPublicChannel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytChannel(
        val id: Long,
        val parentChannelId: Long?,
        val uri: String?,
        val type: String,
        val subject: String?,
        val avatarUrl: String?,
        val metadata: String?,
        val createdAt: Long,
        val updatedAt: Long,
        val messagesClearedAt: Long,
        val memberCount: Long,
        val createdBy: User?,
        val userRole: String?,
        val unread: Boolean,
        val newMessageCount: Long,
        val newMentionCount: Long,
        val newReactedMessageCount: Long,
        val hidden: Boolean,
        val archived: Boolean,
        val muted: Boolean,
        val mutedTill: Long?,
        val pinnedAt: Long?,
        val lastReceivedMessageId: Long,
        val lastDisplayedMessageId: Long,
        val messageRetentionPeriod: Long,
        val lastMessage: SceytMessage?,
        val messages: List<SceytMessage>?,
        val members: List<SceytMember>?,
        val newReactions: List<SceytReaction>?,
        val pendingReactions: List<PendingReactionData>?,
        val pending: Boolean,
        val draftMessage: DraftMessage?) : Parcelable {

    val channelSubject: String
        get() = (if (isGroup) subject
        else getPeer()?.getPresentableName()) ?: ""

    val iconUrl: String?
        get() = if (isGroup) avatarUrl
        else getPeer()?.avatarUrl

    val isGroup get() = stringToEnum(type).isGroup()

    val isPublicChannel get() = stringToEnum(type).isPublicChannel()

    val pinned get() = pinnedAt != null && pinnedAt != 0L

    val autoDeleteEnabled get() = messageRetentionPeriod > 0

    @IgnoredOnParcel
    var typingData: ChannelTypingEventData? = null

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytChannel) return false
        return other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}