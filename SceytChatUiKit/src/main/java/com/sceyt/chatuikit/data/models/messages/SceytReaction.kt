package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import com.vanniktech.ui.Parcelize

@Parcelize
data class SceytReaction(
        val id: Long,
        val messageId: Long,
        val key: String,
        val score: Int,
        val reason: String,
        val createdAt: Long,
        val user: User?,
        val pending: Boolean
) : Parcelable