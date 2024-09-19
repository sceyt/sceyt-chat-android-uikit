package com.sceyt.chatuikit.styles.common

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_FONT_RESOURCE
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class TextStyle(
        @ColorInt val color: Int = UNSET_COLOR,
        @Px val size: Int = UNSET_SIZE,
        val style: Int = Typeface.NORMAL,
        @FontRes val font: Int = UNSET_FONT_RESOURCE
) {

    fun apply(textView: TextView) {
        if (size != UNSET_SIZE) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        }
        if (color != UNSET_COLOR) {
            textView.setTextColor(color)
        }

        val typeface = if (font != UNSET_FONT_RESOURCE)
            ResourcesCompat.getFont(textView.context, font) else Typeface.DEFAULT

        textView.setTypeface(typeface, style)
    }
}