package com.sceyt.chatuikit.presentation.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.sceyt.chatuikit.presentation.common.spans.RoundedBackgroundSpan
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class ClickableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var doOnLongClick: ((View) -> Unit)? = null
    private var doOnClickWhenNoLink: ((View) -> Unit)? = null
    private var doOnSpanLongClick: ((ClickableSpan, View) -> Unit)? = null
    private var pressedSpanBackgroundSpan: RoundedBackgroundSpan? = null
    private var lastTouchEvent: MotionEvent? = null

    private val rippleColor by lazy {
        val baseColor = linkTextColors.defaultColor
        // Add alpha transparency for ripple effect (20% opacity)
        (baseColor and 0x00FFFFFF) or 0x33000000
    }
    private val cornerRadius by lazy { 4f * resources.displayMetrics.density }

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                val spannable = text as? Spannable ?: return false
                val span = getClickableSpan(event)
                if (span != null) {
                    span.onClick(this@ClickableTextView)
                } else {
                    doOnClickWhenNoLink?.invoke(this@ClickableTextView)
                }
                removeRippleEffect(spannable)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val spannable = text as? Spannable
                spannable?.let { removeRippleEffect(it) }

                val span = lastTouchEvent?.let { getClickableSpan(it) }

                when {
                    span == null -> {
                        doOnLongClick?.invoke(this@ClickableTextView)
                    }

                    span is LongClickableSpan -> {
                        span.onLongClick(this@ClickableTextView)
                    }

                    doOnSpanLongClick != null -> {
                        doOnSpanLongClick?.invoke(span, this@ClickableTextView)
                    }

                    else -> {
                        doOnLongClick?.invoke(this@ClickableTextView)
                    }
                }
            }

            override fun onDown(e: MotionEvent): Boolean {
                lastTouchEvent = MotionEvent.obtain(e)
                return true
            }
        })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (text is Spannable) {
            val spannable = text as Spannable

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    getClickableSpan(event)?.let {
                        addRippleEffect(spannable, it)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    removeRippleEffect(spannable)
                }

                MotionEvent.ACTION_CANCEL -> {
                    removeRippleEffect(spannable)
                }
            }

            // Let GestureDetector handle tap and long press
            gestureDetector.onTouchEvent(event)
            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    private fun getClickableSpan(event: MotionEvent): ClickableSpan? {
        val spannable = text as? Spannable ?: return null
        val x = (event.x + scrollX).toInt()
        val y = (event.y + scrollY).toInt()
        val layout = layout ?: return null
        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x.toFloat())
        val links = spannable.getSpans(off, off, ClickableSpan::class.java)
        return links.firstOrNull()
    }

    private fun addRippleEffect(spannable: Spannable, clickableSpan: ClickableSpan) {
        // Find the start and end of the clickable span
        val spanStart = spannable.getSpanStart(clickableSpan)
        val spanEnd = spannable.getSpanEnd(clickableSpan)

        if (spanStart != -1 && spanEnd != -1) {
            // Create and apply rounded background span for ripple effect
            pressedSpanBackgroundSpan = RoundedBackgroundSpan(
                backgroundColor = rippleColor,
                cornerRadius = cornerRadius,
                spanStart = spanStart,
                spanEnd = spanEnd
            )
            spannable.setSpan(
                pressedSpanBackgroundSpan,
                spanStart,
                spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun removeRippleEffect(spannable: Spannable) {
        pressedSpanBackgroundSpan?.let {
            spannable.removeSpan(it)
            pressedSpanBackgroundSpan = null
        }
    }

    fun doOnLongClick(onClick: (View) -> Unit) {
        doOnLongClick = onClick
    }

    fun doOnClickWhenNoLink(onClick: (View) -> Unit) {
        doOnClickWhenNoLink = onClick
    }

    fun doOnSpanLongClick(onClick: (ClickableSpan, View) -> Unit) {
        doOnSpanLongClick = onClick
    }

    fun applyStyle(itemStyle: MessageItemStyle) {
        itemStyle.bodyTextStyle.apply(this)
        setLinkTextColor(itemStyle.linkTextColor)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (text as? Spannable)?.let { removeRippleEffect(it) }
        lastTouchEvent?.recycle()
        lastTouchEvent = null
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            (text as? Spannable)?.let { removeRippleEffect(it) }
        }
    }
}
