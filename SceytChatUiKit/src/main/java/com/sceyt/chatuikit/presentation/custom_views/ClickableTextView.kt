package com.sceyt.chatuikit.presentation.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.sceyt.chatuikit.presentation.common.RoundedBackgroundSpan
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class ClickableTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var doOnLongClick: ((View) -> Unit)? = null
    private var doOnClickWhenNoLink: ((View) -> Unit)? = null
    private var pressedSpanBackgroundSpan: RoundedBackgroundSpan? = null

    private val rippleColor by lazy {
        val baseColor = linkTextColors.defaultColor
        // Add alpha transparency for ripple effect (20% opacity)
        (baseColor and 0x00FFFFFF) or 0x33000000
    }
    private val cornerRadius by lazy { 4f * resources.displayMetrics.density }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            val spannableString = text as? SpannableString ?: return false
            val span = getClickableSpan(event)
            if (span != null) {
                span.onClick(this@ClickableTextView)
            } else {
                doOnClickWhenNoLink?.invoke(this@ClickableTextView)
            }
            removeRippleEffect(spannableString)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val spannableString = text as? SpannableString
            spannableString?.let { removeRippleEffect(it) }
            doOnLongClick?.invoke(this@ClickableTextView)
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (text is SpannableString) {
            val spannableString = text as SpannableString

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    /*getClickableSpan(event)?.let {
                        addRippleEffect(spannableString, it)
                    }*/
                }

                MotionEvent.ACTION_UP -> {
                    removeRippleEffect(spannableString)
                }

                MotionEvent.ACTION_CANCEL -> {
                    removeRippleEffect(spannableString)
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
        val spannableString = text as? SpannableString ?: return null
        val x = (event.x + scrollX).toInt()
        val y = (event.y + scrollY).toInt()
        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x.toFloat())
        val links = spannableString.getSpans(off, off, ClickableSpan::class.java)
        return links.firstOrNull()
    }

    private fun addRippleEffect(spannableString: SpannableString, clickableSpan: ClickableSpan) {
        // Find the start and end of the clickable span
        val spanStart = spannableString.getSpanStart(clickableSpan)
        val spanEnd = spannableString.getSpanEnd(clickableSpan)

        if (spanStart != -1 && spanEnd != -1) {
            // Create and apply rounded background span for ripple effect
            pressedSpanBackgroundSpan = RoundedBackgroundSpan(
                backgroundColor = rippleColor,
                cornerRadius = cornerRadius
            )
            spannableString.setSpan(
                pressedSpanBackgroundSpan,
                spanStart,
                spanEnd,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun removeRippleEffect(spannableString: SpannableString) {
        pressedSpanBackgroundSpan?.let {
            spannableString.removeSpan(it)
            pressedSpanBackgroundSpan = null
        }
    }

    fun doOnLongClick(onClick: (View) -> Unit) {
        doOnLongClick = onClick
    }

    fun doOnClickWhenNoLink(onClick: (View) -> Unit) {
        doOnClickWhenNoLink = onClick
    }

    fun applyStyle(itemStyle: MessageItemStyle) {
        itemStyle.bodyTextStyle.apply(this)
        setLinkTextColor(itemStyle.linkTextColor)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (text as? SpannableString)?.let { removeRippleEffect(it) }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            (text as? SpannableString)?.let { removeRippleEffect(it) }
        }
    }
}