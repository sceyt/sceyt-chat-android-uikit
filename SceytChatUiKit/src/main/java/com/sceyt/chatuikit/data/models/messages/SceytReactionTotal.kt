package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytReactionTotal(
        val key: String,
        val score: Int = 1,
        val containsSelf: Boolean = false
) : Parcelable