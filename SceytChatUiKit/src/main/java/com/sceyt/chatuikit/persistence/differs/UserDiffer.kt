package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.data.hasDiff
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.extensions.equalsIgnoreNull

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

fun SceytUser.diff(other: SceytUser): UserDiff {
    return UserDiff(
        firstNameChanged = !firstName.equalsIgnoreNull(other.firstName),
        lastNameChanged = !lastName.equalsIgnoreNull(other.lastName),
        avatarURLChanged = !avatarURL.equalsIgnoreNull(other.avatarURL),
        presenceChanged = presence?.hasDiff(other.presence) == true,
        activityStatusChanged = state != other.state,
        blockedChanged = blocked != other.blocked
    )
}
