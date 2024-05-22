package com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters

sealed class UserItem {
    data class User(
            val user: com.sceyt.chat.models.user.User
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
