package com.sceyt.chatuikit.styles.common

import android.widget.TextView
import com.sceyt.chatuikit.styles.StyleConstants

data class HintStyle(
        val textColor: Int = StyleConstants.UNSET_COLOR,
        val hint: String = StyleConstants.UNSET_TEXT
){
    fun apply(textInput: TextView) {
        if (textColor != StyleConstants.UNSET_COLOR) {
            textInput.setHintTextColor(textColor)
        }
        if (hint != StyleConstants.UNSET_TEXT) {
            textInput.setHint(hint)
        }
    }
}
