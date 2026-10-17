package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelEventData(
    val channel: SceytChannel,
    val user: SceytUser,
    val activity: ChannelEventEnum,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return other is ChannelEventData && other.user.id == user.id && other.channel.id == channel.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode() * 31 + channel.id.hashCode()
    }
}