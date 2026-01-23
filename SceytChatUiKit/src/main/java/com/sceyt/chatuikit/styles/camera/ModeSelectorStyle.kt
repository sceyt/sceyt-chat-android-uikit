package com.sceyt.chatuikit.styles.camera

import androidx.annotation.ColorInt
import com.sceyt.chatuikit.styles.common.TextStyle

data class ModeSelectorStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val selectedTextColor: Int,
    @param:ColorInt val unselectedTextColor: Int,
    val highlightBackground: android.graphics.drawable.Drawable?,
    val photoText: String,
    val videoText: String,
    val textStyle: TextStyle,
)