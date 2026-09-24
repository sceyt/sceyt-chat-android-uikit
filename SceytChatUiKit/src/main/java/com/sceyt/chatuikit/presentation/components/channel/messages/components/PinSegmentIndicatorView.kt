package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.extensions.dpToPx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The vertical bar on the leading edge of the pinned-messages banner: one segment per pin,
 * with the current one highlighted.
 *
 * Up to [MAX_EQUAL_SEGMENTS] every pin gets a segment and they share the bar's height. Past
 * that the bar becomes a scrolled window showing [VISIBLE_SEGMENTS] at a time — the trailing
 * half segment is the cue that the list runs on — offset to keep the selection centred.
 */
class PinSegmentIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val spacing = dpToPx(2f).toFloat()
    private val cornerRadius = dpToPx(1f).toFloat()

    private var count: Int = 0
    private var selectedIndex: Int = 0

    /**
     * Where the window is actually drawn, which trails [selectedIndex] while the scroll
     * animates. Fractional on purpose: the bar slides between segments rather than stepping.
     */
    private var scrolledToIndex = 0f
    private var scrollAnimator: ValueAnimator? = null

    @ColorInt
    private var activeColor: Int = 0

    @ColorInt
    private var inactiveColor: Int = 0

    fun setColors(@ColorInt active: Int, @ColorInt inactive: Int) {
        activeColor = active
        inactiveColor = inactive
        invalidate()
    }

    fun setSegments(count: Int, selectedIndex: Int) {
        val clamped = selectedIndex.coerceIn(0, max(0, count - 1))
        if (this.count == count && this.selectedIndex == clamped) return
        val sameSet = this.count == count
        this.count = count
        this.selectedIndex = clamped

        val isNeighbour = sameSet && abs(clamped - scrolledToIndex) <= 1f
        if (isNeighbour) animateScrollTo(clamped.toFloat()) else snapScrollTo(clamped.toFloat())
    }

    private fun animateScrollTo(index: Float) {
        scrollAnimator?.cancel()
        scrollAnimator = ValueAnimator.ofFloat(scrolledToIndex, index).apply {
            duration = SCROLL_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                scrolledToIndex = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun snapScrollTo(index: Float) {
        scrollAnimator?.cancel()
        scrollAnimator = null
        scrolledToIndex = index
        invalidate()
    }

    override fun onDetachedFromWindow() {
        scrollAnimator?.cancel()
        scrollAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (count <= 0 || height == 0) return

        if (count == 1) {
            paint.color = activeColor
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            return
        }

        val visible = min(count.toFloat(), VISIBLE_SEGMENTS)
        val segmentHeight = (height - spacing * (visible - 1)) / visible
        val step = segmentHeight + spacing

        val offset = if (count <= MAX_EQUAL_SEGMENTS) 0f else {
            val centred = (scrolledToIndex - (visible - 1) / 2f) * step
            centred.coerceIn(0f, max(0f, count * step - spacing - height))
        }

        for (i in 0 until count) {
            val top = i * step - offset
            val bottom = top + segmentHeight
            if (bottom < 0f || top > height) continue

            paint.color = if (i == selectedIndex) activeColor else inactiveColor
            rect.set(0f, max(0f, top), width.toFloat(), min(height.toFloat(), bottom))
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    private companion object {
        /** Below this, every segment is drawn and they share the bar's height. */
        const val MAX_EQUAL_SEGMENTS = 3

        /** The trailing half signals that the list continues. */
        const val VISIBLE_SEGMENTS = 3.5f

        /** Long enough to read as a scroll, short enough not to lag behind a quick walk. */
        const val SCROLL_DURATION_MS = 180L
    }
}
