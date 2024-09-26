package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.widget.EditText
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class TextInputStyle(
        val backgroundColor: Int = UNSET_COLOR,
        val textStyle: TextStyle = TextStyle(),
        val hintStyle: HintStyle = HintStyle(),
) {
    fun apply(textInput: EditText) {
        textStyle.apply(textInput)
        hintStyle.apply(textInput)
        if (backgroundColor != UNSET_COLOR)
            textInput.setBackgroundColor(backgroundColor)
    }

    internal class Builder(private val typedArray: TypedArray) {
        private var backgroundColor: Int = UNSET_COLOR
        private var textStyle: TextStyle = TextStyle()
        private var hintStyle: HintStyle = HintStyle()

        fun setBackgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            backgroundColor = typedArray.getColor(index, defValue)
        }

        fun setTextStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun setHintStyle(hintStyle: HintStyle) = apply {
            this.hintStyle = hintStyle
        }

        fun build() = TextInputStyle(
            backgroundColor = backgroundColor,
            textStyle = textStyle,
            hintStyle = hintStyle
        )
    }
}
