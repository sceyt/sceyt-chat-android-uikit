package com.sceyt.sceytchatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.models.messages.PendingReactionData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isGroup
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytChannel(
        var id: Long,
        val parentChannelId: Long?,
        var uri: String?,
        val type: String,
        var subject: String?,
        var avatarUrl: String?,
        var metadata: String?,
        var createdAt: Long,
        var updatedAt: Long,
        var messagesClearedAt: Long,
        var memberCount: Long,
        val createdBy: User?,
        var userRole: String?,
        var unread: Boolean,
        var newMessageCount: Long,
        var newMentionCount: Long,
        var newReactedMessageCount: Long,
        var hidden: Boolean,
        var archived: Boolean,
        var muted: Boolean,
        var mutedTill: Long?,
        var pinnedAt: Long?,
        var lastReceivedMessageId: Long,
        var lastDisplayedMessageId: Long,
        var messageRetentionPeriod: Long,
        var lastMessage: SceytMessage?,
        var messages: List<SceytMessage>?,
        var members: List<SceytMember>?,
        var newReactions: List<SceytReaction>?,
        var pendingReactions: List<PendingReactionData>?,
        var pending: Boolean) : Parcelable, Cloneable {

    val channelSubject: String
        get() = (if (isGroup) subject
        else getPeer()?.getPresentableName()) ?: ""

    val iconUrl: String?
        get() = if (isGroup) avatarUrl
        else getPeer()?.avatarUrl

    val isGroup get() = stringToEnum(type).isGroup()

    @IgnoredOnParcel
    var typingData: ChannelTypingEventData? = null

    @IgnoredOnParcel
    var draftMessage: DraftMessage? = null

    fun getSubjectAndAvatarUrl(): Pair<String, String?> {
        return Pair(channelSubject, iconUrl)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytChannel) return false
        return other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    public override fun clone(): SceytChannel {
        return SceytChannel(
            id = id,
            parentChannelId = parentChannelId,
            uri = uri,
            type = type,
            subject = subject,
            avatarUrl = avatarUrl,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            messagesClearedAt = messagesClearedAt,
            memberCount = memberCount,
            createdBy = createdBy,
            userRole = userRole,
            unread = unread,
            newMessageCount = newMessageCount,
            newMentionCount = newMentionCount,
            newReactedMessageCount = newReactedMessageCount,
            hidden = hidden,
            archived = archived,
            muted = muted,
            mutedTill = mutedTill,
            pinnedAt = pinnedAt,
            lastReceivedMessageId = lastReceivedMessageId,
            lastDisplayedMessageId = lastDisplayedMessageId,
            messageRetentionPeriod = messageRetentionPeriod,
            lastMessage = lastMessage?.clone(),
            messages = messages?.map { it.clone() },
            members = members?.map { it.clone() },
            newReactions = newReactions,
            pendingReactions = pendingReactions,
            pending = pending
        ).also {
            it.draftMessage = draftMessage?.copy()
        }
    }
}