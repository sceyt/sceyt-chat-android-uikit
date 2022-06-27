package com.sceyt.chat.ui.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.UserActivityStatus
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMember(
        var id: String,
        var role: Role,
        val firstName: String?,
        val lastName: String?,
        val avatarURL: String?,
        val metadata: String?,
        val presence: Presence?,
        val activityStatus: UserActivityStatus?,
        val blocked: Boolean?,
) : Parcelable {
    val fullName: String
        get() = (firstName ?: ("" + lastName)).trim()
}
