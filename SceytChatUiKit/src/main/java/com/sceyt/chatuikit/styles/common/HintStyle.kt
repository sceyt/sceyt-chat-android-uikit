package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.widget.TextView
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants

data class HintStyle(
        val textColor: Int = StyleConstants.UNSET_COLOR,
        val hint: String = StyleConstants.UNSET_TEXT
) {
    fun apply(textInput: TextView) {
        if (textColor != StyleConstants.UNSET_COLOR) {
            textInput.setHintTextColor(textColor)
        }
        if (hint != StyleConstants.UNSET_TEXT) {
            textInput.setHint(hint)
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        private var textColor: Int = StyleConstants.UNSET_COLOR
        private var hint: String = StyleConstants.UNSET_TEXT

        fun textColor(@StyleableRes index: Int, defValue: Int = textColor) = apply {
            textColor = typedArray.getColor(index, defValue)
        }

        fun hint(@StyleableRes index: Int, defValue: String = hint) = apply {
            hint = typedArray.getString(index) ?: defValue
        }

        fun build() = HintStyle(
            textColor = textColor,
            hint = hint
        )
    }
}
