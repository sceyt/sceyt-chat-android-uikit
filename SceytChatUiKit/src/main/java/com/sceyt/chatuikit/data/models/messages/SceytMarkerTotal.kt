package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytMarkerTotal(
        val name: String,
        val count: Long
) : Parcelable