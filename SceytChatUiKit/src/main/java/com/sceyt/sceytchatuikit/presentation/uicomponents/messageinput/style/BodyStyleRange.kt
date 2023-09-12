package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BodyStyleRange(
        val loc: Int,
        val len: Int,
        val style: StyleType
) : Parcelable