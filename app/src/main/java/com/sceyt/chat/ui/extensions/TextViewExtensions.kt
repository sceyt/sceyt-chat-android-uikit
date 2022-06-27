package com.sceyt.chat.ui.extensions

import androidx.appcompat.widget.AppCompatTextView

fun AppCompatTextView.setDrawableEnd(id: Int) {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, id, 0)
}