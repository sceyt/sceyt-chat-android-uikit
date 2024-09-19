package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Icon
import androidx.annotation.ColorInt

data class MediaLoaderStyle(
    @ColorInt val trackColor: Int,
    @ColorInt val progressColor: Int,
    @ColorInt val backgroundColor: Int?,
    val cancelIcon: Icon?,
    val uploadIcon: Icon?,
    val downloadIcon: Icon?

)
