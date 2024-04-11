package com.sceyt.chatuikit.presentation.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatTextView
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.sceytstyles.MessagesStyle


class SceytClickableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var doOnLongClick: ((View) -> Unit)? = null
    private var doOnClickWhenNoLink: ((View) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isLongClick = false
    private val longClickRunnable = Runnable {
        isLongClick = true
        doOnLongClick?.invoke(this)
    }

    init {
        setLinkTextColor(context.getCompatColor(MessagesStyle.autoLinkTextColor))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (text is SpannableString) {
            val spannableString = text as SpannableString
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longClickRunnable)
                    if (!isLongClick) {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        val line = layout.getLineForVertical(y)
                        val off = layout.getOffsetForHorizontal(line, x.toFloat())

                        val link = spannableString.getSpans(off, off, ClickableSpan::class.java)

                        if (link.isNotEmpty()) {
                            link[0].onClick(this)
                            return true
                        } else doOnClickWhenNoLink?.invoke(this)
                    }
                    isLongClick = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longClickRunnable)
                }
            }

            return true
        } else return super.onTouchEvent(event)
    }

    fun doOnLongClick(onClick: (View) -> Unit) {
        doOnLongClick = onClick
    }

    fun doOnClickWhenNoLink(onClick: (View) -> Unit) {
        doOnClickWhenNoLink = onClick
    }
}