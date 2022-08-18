package com.sceyt.chat.ui.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMember(
        var role: Role,
        val user: User,
) : Parcelable {

    constructor(user: User) : this(Role("participant"), user)

    val fullName: String
        get() = "${user.firstName} ${user.lastName}".trim()

    val avatarUrl: String
        get() = user.avatarURL

    val id: String get() = user.id

    override fun equals(other: Any?): Boolean {
        return other != null && other is SceytMember && other.id == id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
