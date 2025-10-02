package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelEventData(
        val user: SceytUser,
        val activity: ChannelEventEnum,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return other is ChannelEventData && other.user.id == user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}