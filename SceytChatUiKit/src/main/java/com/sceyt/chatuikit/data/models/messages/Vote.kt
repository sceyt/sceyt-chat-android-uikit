package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Vote(
    val optionId: String,
    val createdAt: Long,
    val user: SceytUser?,
) : Parcelable