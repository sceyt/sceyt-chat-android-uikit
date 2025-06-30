package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.UserActivity

data class ActiveUser(
        val user: SceytUser,
        val activity: UserActivity
) {
    override fun equals(other: Any?): Boolean {
        return other is ActiveUser && other.user.id == user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}