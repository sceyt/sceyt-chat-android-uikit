package com.sceyt.chatuikit.styles.common

import android.widget.EditText
import com.sceyt.chatuikit.styles.StyleConstants

data class TextInputStyle(
        val backgroundColor: Int = StyleConstants.UNSET_COLOR,
        val textStyle: TextStyle = TextStyle(),
        val hintStyle: HintStyle = HintStyle(),
) {
    fun apply(textInput: EditText) {
        textStyle.apply(textInput)
        hintStyle.apply(textInput)
        textInput.setBackgroundColor(backgroundColor)
    }
}
