package com.sceyt.sceytchatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import kotlinx.parcelize.IgnoredOnParcel
import java.util.*

abstract class SceytChannel(open var id: Long,
                            open var createdAt: Long,
                            open var updatedAt: Long,
                            open var unreadMessageCount: Long,
                            open var unreadMentionCount: Long,
                            open var unreadReactionCount: Long,
                            open var lastMessage: SceytMessage?,
                            open var label: String?,
                            open var metadata: String?,
                            open var muted: Boolean,
                            open var muteExpireDate: Date?,
                            open var markedUsUnread: Boolean,
                            open var lastDeliveredMessageId: Long,
                            open var lastReadMessageId: Long,
                            open var channelType: ChannelTypeEnum,
                            open var messagesDeletionDate: Long,
                            open var lastMessages: List<SceytMessage>?) : Parcelable, Cloneable {

    @IgnoredOnParcel
    open val channelSubject = ""

    @IgnoredOnParcel
    open val iconUrl: String? = ""

    @IgnoredOnParcel
    open val isGroup = false

    var typingData: ChannelTypingEventData? = null

    fun getSubjectAndAvatarUrl(): Pair<String, String?> {
        return when (this) {
            is SceytDirectChannel -> Pair(peer?.getPresentableName() ?: "", peer?.avatarUrl)
            is SceytGroupChannel -> Pair(subject ?: "", avatarUrl)
            else -> Pair("", null)
        }
    }

    fun getChannelAvatarUrl(): String? {
        return when (this) {
            is SceytDirectChannel -> peer?.avatarUrl
            is SceytGroupChannel -> avatarUrl
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytChannel) return false
        return other.id == id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + unreadMessageCount.hashCode()
        result = 31 * result + (lastMessage?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + muted.hashCode()
        result = 31 * result + (muteExpireDate?.hashCode() ?: 0)
        result = 31 * result + channelType.hashCode()
        return result
    }

    public abstract override fun clone(): SceytChannel
}