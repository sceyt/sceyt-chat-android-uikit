package com.sceyt.chatuikit.styles

import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

@IntDef(value = [Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC, UNSET_STYLE])
@Retention(AnnotationRetention.SOURCE)
annotation class Style

object StyleConstants {
    const val UNSET_SIZE = -1
    const val UNSET_COLOR = Integer.MIN_VALUE
    const val UNSET_RESOURCE = -1
    const val UNSET_CORNER_RADIUS = -1f
    const val UNSET_BORDER_WIDTH = -1f
    const val UNSET_TEXT = ""
    const val UNSET_STYLE = -1


    fun Int.colorOrDefault(@ColorInt default: Int): Int {
        return if (this == UNSET_COLOR) default else this
    }

    fun Int.sizeOrDefault(default: Int): Int {
        return if (this == UNSET_SIZE) default else this
    }

    fun Int.fontOrDefault(default: Int): Int {
        return if (this == UNSET_RESOURCE) default else this
    }

    fun Int.styleOrDefault(@Style default: Int): Int {
        return if (this == UNSET_STYLE) default else this
    }
}