package com.sceyt.chatuikit.styles

import androidx.annotation.ColorInt

object StyleConstants {
    const val UNSET_SIZE = -1
    const val UNSET_COLOR = Integer.MIN_VALUE
    const val UNSET_FONT_RESOURCE = -1
    const val UNSET_CORNER_RADIUS = -1f
    const val UNSET_TEXT = ""
    const val UNSET_STYLE = -1


    fun Int.colorOrDefault(@ColorInt default: Int): Int {
        return if (this == UNSET_COLOR) default else this
    }

    fun Int.sizeOrDefault(default: Int): Int {
        return if (this == UNSET_SIZE) default else this
    }

    fun Int.fontOrDefault(default: Int): Int {
        return if (this == UNSET_FONT_RESOURCE) default else this
    }

    fun Int.styleOrDefault(default: Int): Int {
        return if (this == UNSET_STYLE) default else this
    }
}