package com.sceyt.chatuikit.shared.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

class SceytClearFocusEditText : AppCompatEditText {
    var emptyDeleteListener: (() -> Boolean)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val baseConnection = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(baseConnection, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength == 1 && afterLength == 0 && shouldHandleEmptyDelete()) {
                    return emptyDeleteListener?.invoke() == true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_DEL &&
                    shouldHandleEmptyDelete()
                ) {
                    return emptyDeleteListener?.invoke() == true
                }
                return super.sendKeyEvent(event)
            }
        }
    }

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && shouldHandleEmptyDelete()) {
            if (emptyDeleteListener?.invoke() == true) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun shouldHandleEmptyDelete(): Boolean {
        return selectionStart == 0 && selectionEnd == 0 && text.isNullOrEmpty()
    }
}
