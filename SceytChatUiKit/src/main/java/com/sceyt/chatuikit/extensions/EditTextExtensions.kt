package com.sceyt.chatuikit.extensions

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged

fun EditText.doAfterRealTextChanged(onChanged: (Editable) -> Unit): TextWatcher {
    var lastText: String? = text.toString()

    return doAfterTextChanged { editable ->
        val currentText = editable?.toString() ?: return@doAfterTextChanged
        if (currentText != lastText) {
            lastText = currentText
            onChanged(editable)
        }
    }
}

fun EditText.setMultiLineWithImeOptions(
    imeOptions: Int = EditorInfo.IME_ACTION_DONE,
) {
    this.imeOptions = imeOptions
    setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
}