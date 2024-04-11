package com.sceyt.chatuikit.shared.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

class SceytClearFocusEditText : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            // Hide cursor
            isFocusable = false
            // Set EditText to be focusable again
            isFocusable = true
            isFocusableInTouchMode = true
        }
        return super.onKeyPreIme(keyCode, event)
    }
}