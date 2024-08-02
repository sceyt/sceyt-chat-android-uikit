package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMarker(
        val messageId: Long,
        val userId: String,
        val user: User?,
        val name: String,
        val createdAt: Long,
) : Parcelable {

    constructor(messageId: Long, user: User, name: String, createdAt: Long) : this(messageId, user.id, user, name, createdAt)
}