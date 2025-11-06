package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PollOption(
    val id: String,
    val name: String,
    val order: Int,
) : Parcelable