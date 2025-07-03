package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum

data class ChannelEventData(
        val user: SceytUser,
        val activity: ChannelEventEnum
) {
    override fun equals(other: Any?): Boolean {
        return other is ChannelEventData && other.user.id == user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}