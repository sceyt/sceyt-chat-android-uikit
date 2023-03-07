package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull

fun User.diff(other: User): UserDiff {
    return UserDiff(
        firstNameChanged = firstName.equalsIgnoreNull(other.firstName).not(),
        lastNameChanged = lastName.equalsIgnoreNull(other.lastName).not(),
        avatarURLChanged = avatarURL.equalsIgnoreNull(other.avatarURL).not(),
        presenceChanged = presence?.state != other.presence.state,
        activityStatusChanged = activityState != other.activityState,
        blockedChanged = blocked != other.blocked
    )
}

data class UserDiff(
        val firstNameChanged: Boolean,
        val lastNameChanged: Boolean,
        val avatarURLChanged: Boolean,
        val presenceChanged: Boolean,
        val activityStatusChanged: Boolean,
        val blockedChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return firstNameChanged || lastNameChanged || avatarURLChanged || presenceChanged ||
                activityStatusChanged || blockedChanged
    }

    companion object {
        val DEFAULT = UserDiff(
            firstNameChanged = true,
            lastNameChanged = true,
            avatarURLChanged = true,
            presenceChanged = true,
            activityStatusChanged = true,
            blockedChanged = true
        )
    }
}
