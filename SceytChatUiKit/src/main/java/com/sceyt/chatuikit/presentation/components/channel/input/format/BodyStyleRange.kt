package com.sceyt.chatuikit.presentation.components.channel.input.format

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BodyStyleRange(
        val offset: Int,
        val length: Int,
        val style: StyleType
) : Parcelable