package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.models.messages.SceytUser

data class ActiveUser(
        val user: SceytUser,
        val activity: UserActivityState
) {
    override fun equals(other: Any?): Boolean {
        return other is ActiveUser && other.user.id == user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}