package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.UserState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytUser(
        val id: String,
        val username: String,
        val firstName: String,
        val lastName: String,
        val avatarURL: String?,
        val metadataMap: Map<String, String>?,
        val presence: Presence?,
        val state: UserState,
        val blocked: Boolean
) : Parcelable {

    val fullName: String
        get() = "$firstName $lastName".trim()

    constructor(id: String) : this(
        id = id,
        username = "",
        firstName = "",
        lastName = "",
        avatarURL = "",
        metadataMap = null,
        presence = null,
        state = UserState.Active,
        blocked = false)

    override fun equals(other: Any?): Boolean {
        return other is SceytUser && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}