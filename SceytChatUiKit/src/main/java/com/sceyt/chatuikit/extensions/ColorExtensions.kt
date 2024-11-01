package com.sceyt.chatuikit.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit

@ColorInt
fun Int.withAlpha(alpha: Float): Int {
    if (alpha !in 0f..1f) {
        throw IllegalArgumentException("Alpha must be in the range 0.0 to 1.0")
    }
    val newAlpha = (alpha * 255).toInt().coerceIn(0, 255)  // Ensure alpha is in the valid range
    return Color.argb(newAlpha, Color.red(this), Color.green(this), Color.blue(this))
}

fun Context.createColorState(
        @ColorInt checkedColor: Int = getCompatColor(SceytChatUIKit.theme.colors.accentColor),
        @ColorInt uncheckedColor: Int = getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor),
) = ColorStateList(
    arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf(-android.R.attr.state_checked)
    ),
    intArrayOf(
        checkedColor,
        uncheckedColor
    )
)
