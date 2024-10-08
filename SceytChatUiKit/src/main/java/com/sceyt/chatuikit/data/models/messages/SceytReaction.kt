package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytReaction(
        val id: Long,
        val messageId: Long,
        val key: String,
        val score: Int,
        val reason: String,
        val createdAt: Long,
        val user: SceytUser?,
        val pending: Boolean
) : Parcelable