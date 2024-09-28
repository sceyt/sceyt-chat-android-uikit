package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMarker(
        val messageId: Long,
        val userId: String,
        val user: SceytUser?,
        val name: String,
        val createdAt: Long,
) : Parcelable {

    constructor(messageId: Long,
                user: SceytUser,
                name: String,
                createdAt: Long) : this(messageId, user.id, user, name, createdAt)
}