package com.sceyt.sceytchatuikit.extensions

import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible

fun AppCompatTextView.setDrawableEnd(id: Int) {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, id, 0)
}

fun TextView.setTextAndVisibility(title: String?) {
    if (title.isNullOrBlank()) {
        isVisible = false
    } else {
        text = title
        isVisible = true
    }
}