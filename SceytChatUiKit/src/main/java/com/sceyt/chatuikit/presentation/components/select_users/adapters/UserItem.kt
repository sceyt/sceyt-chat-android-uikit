package com.sceyt.chatuikit.presentation.components.select_users.adapters

import com.sceyt.chatuikit.data.models.messages.SceytUser

sealed class UserItem {
    data class User(
            val user: SceytUser
    ) : UserItem() {
        var chosen = false
    }

    object LoadingMore : UserItem()

    override fun equals(other: Any?): Boolean {
        return when {
            this is User && other is User -> {
                return user.id == other.user.id
            }

            else -> super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
