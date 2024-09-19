package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import android.widget.EditText
import com.sceyt.chatuikit.styles.StyleConstants

data class SearchInputStyle(
        val backgroundColor: Int = StyleConstants.UNSET_COLOR,
        val searchIcon: Drawable? = null,
        val clearIcon: Drawable? = null,
        val textStyle: TextStyle = TextStyle(),
) {
    fun apply(editText: EditText) {
        textStyle.apply(editText)
        editText.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null)
        editText.setBackgroundColor(backgroundColor)
    }
}
