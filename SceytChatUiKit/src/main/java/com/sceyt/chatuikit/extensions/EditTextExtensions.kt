package com.sceyt.chatuikit.extensions

import android.text.Editable
import android.text.TextWatcher
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