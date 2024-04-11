package com.sceyt.chatuikit.presentation.uicomponents.messageinput.style

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BodyStyleRange(
        val offset: Int,
        val length: Int,
        val style: StyleType
) : Parcelable