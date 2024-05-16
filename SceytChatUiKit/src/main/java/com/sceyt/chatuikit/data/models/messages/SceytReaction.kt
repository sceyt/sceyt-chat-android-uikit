package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import com.vanniktech.ui.Parcelize

@Parcelize
data class SceytReaction(
        var id: Long,
        val messageId: Long,
        val key: String,
        var score: Int,
        val reason: String,
        val createdAt: Long,
        var user: User?,
        var pending: Boolean
) : Parcelable