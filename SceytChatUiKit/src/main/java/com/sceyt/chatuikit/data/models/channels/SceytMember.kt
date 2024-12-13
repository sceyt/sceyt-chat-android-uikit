package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMember(
        val role: Role,
        val user: SceytUser,
) : Parcelable {

    constructor(user: SceytUser) : this(Role(SceytChatUIKit.config.memberRolesConfig.participant), user)

    constructor(user: SceytUser, roleName: String) : this(Role(roleName), user)

    val fullName: String
        get() = "${user.firstName} ${user.lastName}".trim()

    val avatarUrl: String?
        get() = user.avatarURL

    val id: String get() = user.id

    override fun equals(other: Any?): Boolean {
        return other != null && other is SceytMember && other.id == id && other.role == role
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
