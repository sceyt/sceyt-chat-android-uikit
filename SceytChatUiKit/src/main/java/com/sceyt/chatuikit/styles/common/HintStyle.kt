package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.widget.TextView
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants

data class HintStyle(
        val color: Int = StyleConstants.UNSET_COLOR,
        val hint: String = StyleConstants.UNSET_TEXT
) {
    fun apply(textInput: TextView) {
        if (color != StyleConstants.UNSET_COLOR) {
            textInput.setHintTextColor(color)
        }
        if (hint != StyleConstants.UNSET_TEXT) {
            textInput.setHint(hint)
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        private var color: Int = StyleConstants.UNSET_COLOR
        private var hint: String = StyleConstants.UNSET_TEXT

        fun color(@StyleableRes index: Int, defValue: Int = color) = apply {
            color = typedArray.getColor(index, defValue)
        }

        fun hint(@StyleableRes index: Int, defValue: String = hint) = apply {
            hint = typedArray.getString(index) ?: defValue
        }

        fun build() = HintStyle(
            color = color,
            hint = hint
        )
    }
}
