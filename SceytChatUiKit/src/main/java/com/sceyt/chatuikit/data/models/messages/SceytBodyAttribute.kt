package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytBodyAttribute(
        val type: String,
        val offset: Int,
        val length: Int,
        val metadata: String?
) : Parcelable