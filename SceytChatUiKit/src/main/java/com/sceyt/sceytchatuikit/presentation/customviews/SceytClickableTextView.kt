package com.sceyt.sceytchatuikit.presentation.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class SceytClickableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatTextView(context, attrs, defStyleAttr) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (text is SpannableString) {
            val spannableString = text as SpannableString
            val action = event.action
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()

                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                val link = spannableString.getSpans(off, off, ClickableSpan::class.java)

                if (link.isNotEmpty()) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(this)
                    }
                    return true
                }
            }
            return false
        } else return super.onTouchEvent(event)
    }
}