package com.sceyt.sceytchatuikit.data.models.channels

import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.*
import com.sceyt.sceytchatuikit.BR

@Parcelize
open class SceytChannel(open var id: Long,
                        open var createdAt: Long,
                        open var updatedAt: Long,
                        open var unreadMessageCount: Long,
                        open var lastMessage: SceytMessage? = null,
                        open var label: String?,
                        open var metadata: String?,
                        open var muted: Boolean,
                        open var muteExpireDate: Date?,
                        open var channelType: ChannelTypeEnum) : BaseObservable(), Parcelable, Cloneable {

    @IgnoredOnParcel
    @Bindable
    var message: SceytMessage? = null
        get() = lastMessage
        set(value) {
            field = value
            lastMessage = value
            notifyPropertyChanged(BR.message)
        }

    @IgnoredOnParcel
    @Bindable
    var unreadCount = 0L
        get() = unreadMessageCount
        set(value) {
            field = value
            unreadMessageCount = value
            notifyPropertyChanged(BR.unreadCount)
        }

    @IgnoredOnParcel
    open val channelSubject = ""

    @IgnoredOnParcel
    open val iconUrl: String? = ""

    @IgnoredOnParcel
    open val isGroup = false

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

    public override fun clone(): SceytChannel {
        return SceytChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage?.clone(), label, metadata, muted, muteExpireDate, channelType)
    }
}