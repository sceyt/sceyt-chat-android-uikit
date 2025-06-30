package com.sceyt.chatuikit.data.managers.channel.event

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.UserActivity
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelMemberActivityEvent(
        val channel: SceytChannel,
        val user: SceytUser,
        val activity: UserActivity,
        val active: Boolean,
) : Parcelable {

    val userId get() = user.id

    val channelId get() = channel.id

    fun inverse(): ChannelMemberActivityEvent {
        return copy(active = !active)
    }
}
