package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

sealed class ChannelMemberActivityEvent(
        val channelId: Long,
        val userId: String,
        val active: Boolean
) {

    data class Typing(
            val channel: SceytChannel,
            val user: SceytUser,
            val typing: Boolean,
    ) : ChannelMemberActivityEvent(channel.id, user.id, typing)

    data class Recording(
            val channel: SceytChannel,
            val user: SceytUser,
            val recording: Boolean,
    ) : ChannelMemberActivityEvent(channel.id, user.id, recording)


    fun inverse(): ChannelMemberActivityEvent {
        return when (this) {
            is Typing -> copy(typing = !typing)
            is Recording -> copy(recording = !recording)
        }
    }
}
