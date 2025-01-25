package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytReactionTotal(
        val key: String,
        val count: Long,
        val score: Int
) : Parcelable