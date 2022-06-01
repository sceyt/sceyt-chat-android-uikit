package com.sceyt.chat.ui.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.extensions.getPresentableName
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
open class SceytChannel(open var id: Long,
                        open var createdAt: Long,
                        open var updatedAt: Long,
                        open var unreadMessageCount: Long,
                        open var lastMessage: Message? = null,
                        open var label: String?,
                        open var metadata: String?,
                        open var muted: Boolean,
                        open var muteExpireDate: Date?,
                        open var channelType: ChannelTypeEnum) : Parcelable {

    fun getSubjectAndAvatarUrl(): Pair<String, String?> {
        return when (this) {
            is SceytDirectChannel -> Pair(peer?.getPresentableName() ?: "", peer?.avatarURL)
            is SceytGroupChannel -> Pair(subject ?: "", avatarUrl)
            else -> Pair("", null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SceytChannel) return false
        return (other.id == id && other.unreadMessageCount == unreadMessageCount
                && other.lastMessage?.id == lastMessage?.id && other.channelType == channelType)
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
}