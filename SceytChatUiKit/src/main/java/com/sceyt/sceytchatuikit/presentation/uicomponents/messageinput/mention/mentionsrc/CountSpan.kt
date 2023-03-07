package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.mentionsrc

import android.text.Layout
import android.text.TextPaint
import android.text.style.CharacterStyle
import java.util.*

/**
 * Span that displays +[count]
 */
class CountSpan : CharacterStyle() {
    var countText = ""
        private set

    override fun updateDrawState(textPaint: TextPaint) {
        //Do nothing, we are using this span as a location marker
    }

    fun setCount(c: Int) {
        countText = if (c > 0) {
            String.format(Locale.getDefault(), " +%d", c)
        } else {
            ""
        }
    }

    fun getCountTextWidthForPaint(paint: TextPaint?): Float {
        return Layout.getDesiredWidth(countText, 0, countText.length, paint)
    }
}