package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytRole
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMember(
        val role: SceytRole,
        val user: SceytUser,
) : Parcelable {

    constructor(user: SceytUser) : this(SceytRole(SceytChatUIKit.config.memberRolesConfig.participant), user)

    constructor(
            userId: String,
            roleName: String = SceytChatUIKit.config.memberRolesConfig.participant
    ) : this(SceytRole(roleName), SceytUser(userId))

    constructor(
            user: SceytUser,
            roleName: String = SceytChatUIKit.config.memberRolesConfig.participant
    ) : this(SceytRole(roleName), user)

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
