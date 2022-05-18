package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.extensions.getCompatDrawable
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

class DateStatusView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var textBoundsRect: Rect
    private lateinit var iconBoundsRect: Rect
    private var dateText = ""
    private var textSize = 30
    private var statusIconSize = 0
    private var statusIconMargin = 0
    private var heightIcon = 0
    private var widthIcon = 0
    private var textColor = Color.BLACK
    private var statusDrawable: Drawable? = null
    private var firstStatusIcon = true
    private var mMargin = 0
    private var mIconSize = 0
    private var isHighlighted = false
    private lateinit var paddings: IntArray

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.DateStatusView)
            statusDrawable = a.getDrawable(R.styleable.DateStatusView_statusIcon)
            dateText = a.getString(R.styleable.DateStatusView_dateText) ?: dateText
            textSize = a.getDimensionPixelSize(R.styleable.DateStatusView_dateTextSize, textSize)
            textColor = a.getColor(R.styleable.DateStatusView_dateTextColor, textColor)
            statusIconMargin = a.getDimensionPixelSize(R.styleable.DateStatusView_statusIconMargin, statusIconMargin)
            statusIconSize = a.getDimensionPixelSize(R.styleable.DateStatusView_statusIconSize, 0)
            firstStatusIcon = a.getBoolean(R.styleable.DateStatusView_firstStatus, firstStatusIcon)
            getPaddingsFromAttr(a)
            a.recycle()
        }
        init()
    }

    private fun getPaddingsFromAttr(typedArray: TypedArray) {
        /** For highlighted state.
         *  After removing state, need to set initial paddings.*/
        paddings = IntArray(4)
        // padding start
        paddings[0] = typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingStart,
            typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingHorizontal, 0))
        // padding top
        paddings[1] = typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingTop,
            typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingVertical, 0))
        // padding end
        paddings[2] = typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingEnd,
            typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingHorizontal, 0))
        // padding bottom
        paddings[3] = typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingBottom,
            typedArray.getDimensionPixelSize(R.styleable.DateStatusView_android_paddingVertical, 0))
    }

    private fun init() {
        textPaint.color = textColor
        textPaint.textSize = textSize.toFloat()
        mMargin = statusIconMargin
        mIconSize = statusIconSize
        textBoundsRect = Rect()
        iconBoundsRect = Rect()

        setHighlightedState(isHighlighted)

        checkSizesAndMargins()
        if (firstStatusIcon)
            measureViewsFirstStatus()
        else measureViewsFirstText()
    }

    private fun measureViewsFirstText() {
        textPaint.getTextBounds(dateText, 0, dateText.length, textBoundsRect)

        if (statusDrawable != null) {
            initStatsIconSize(statusDrawable!!)

            val left = textBoundsRect.right + mMargin - textBoundsRect.left + paddingStart
            val top = getTopFormIcon() + paddingTop
            val sizeDiff = getStatusIconWidthHeightDiff()
            val widthDiff = sizeDiff.first
            val heightDiff = sizeDiff.second

            iconBoundsRect.set(left + widthDiff, top + heightDiff,
                left + widthIcon + widthDiff, top + heightIcon + heightDiff)
        }
    }

    private fun measureViewsFirstStatus() {
        textPaint.getTextBounds(dateText, 0, dateText.length, textBoundsRect)

        statusDrawable?.let {
            initStatsIconSize(it)

            val left = paddingStart
            val sizeDiff = getStatusIconWidthHeightDiff()
            val widthDiff = sizeDiff.first
            val heightDiff = sizeDiff.second

            val top = getTopFormIcon() + paddingTop
            iconBoundsRect.set(left + widthDiff, top + heightDiff,
                left + widthIcon + widthDiff, top + heightIcon + heightDiff)
        }
    }

    private fun getStatusIconWidthHeightDiff(): Pair<Int, Int> {
        val widthDiff = if (widthIcon < heightIcon) (heightIcon - widthIcon) / 2 else 0
        val heightDiff = if (heightIcon < widthIcon && Integer.max(heightIcon, mIconSize) >= textBoundsRect.height()) (widthIcon - heightIcon) / 2 else 0
        return Pair(widthDiff, heightDiff)
    }

    private fun checkSizesAndMargins() {
        if (statusDrawable == null)
            mIconSize = 0

        if (statusDrawable == null || dateText.isBlank())
            mMargin = 0
    }

    private fun initStatsIconSize(icon: Drawable) {
        if (mIconSize == 0) {
            mIconSize = if (textBoundsRect.height() != 0)
                textBoundsRect.height()
            else min(icon.intrinsicWidth, icon.intrinsicHeight)
        }

        if (icon.intrinsicWidth > icon.intrinsicHeight) {
            widthIcon = min(mIconSize, Integer.max(mIconSize, icon.intrinsicWidth))
            heightIcon = widthIcon * icon.intrinsicHeight / icon.intrinsicWidth
        } else {
            heightIcon = min(mIconSize, Integer.max(mIconSize, icon.intrinsicHeight))
            widthIcon = heightIcon * icon.intrinsicWidth / icon.intrinsicHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (firstStatusIcon) {
            //Draw status icon
            statusDrawable?.let {
                it.bounds = iconBoundsRect
                it.draw(canvas)
            }
            //Draw text
            canvas.drawText(dateText,
                -textBoundsRect.left.toFloat() + Integer.max(iconBoundsRect.right, mIconSize) + mMargin,
                (abs(textBoundsRect.top) + getTopFormText() + paddingTop).toFloat(),
                textPaint)
        } else {
            //Draw text
            canvas.drawText(dateText,
                -textBoundsRect.left.toFloat() + paddingStart,
                (abs(textBoundsRect.top) + getTopFormText() + paddingTop).toFloat(),
                textPaint)

            //Draw status icon
            statusDrawable?.let {
                it.bounds = iconBoundsRect
                it.draw(canvas)
            }
        }
    }

    /** For center vertical text date when icon is bigger */
    private fun getTopFormText(): Int {
        val realHeight = Integer.max(heightIcon, mIconSize)
        return if (textBoundsRect.height() < realHeight)
            (realHeight - (textBoundsRect.top.absoluteValue)) / 2
        else 0
    }

    /** For center vertical status icon when text height is bigger*/
    private fun getTopFormIcon(): Int {
        return if (Integer.max(heightIcon, mIconSize) < textBoundsRect.height()) {
            (textBoundsRect.height() - heightIcon) / 2
        } else 0
    }

    private fun setHighlightedState(highlighted: Boolean) {
        if (highlighted) {
            textPaint.color = Color.WHITE
            statusDrawable?.setTint(Color.WHITE)
            background = context.getCompatDrawable(R.drawable.date_transparent_background)
        } else {
            textPaint.color = textColor
            statusDrawable?.setTintList(null)
            background = null
            setPadding(paddings[0], paddings[1], paddings[2], paddings[3])
        }
    }

    fun setStatusIcon(drawable: Drawable?) {
        statusDrawable = drawable
        init()
        requestLayout()
        invalidate()
    }

    fun setDateText(text: String) {
        dateText = text
        init()
        requestLayout()
        invalidate()
    }

    fun setDateAndStatusIcon(text: String, drawable: Drawable?) {
        statusDrawable = drawable?.mutate()
        dateText = text
        init()
        requestLayout()
        invalidate()
    }

    fun setTextColorRes(@ColorRes color: Int) {
        textPaint.color = context.getCompatColorByTheme(color)
        invalidate()
    }

    fun setTextColor(@ColorInt color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setHighlighted(highlighted: Boolean) {
        if (isHighlighted == highlighted) return
        isHighlighted = highlighted
        setHighlightedState(highlighted)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = textBoundsRect.width() + mMargin + Integer.max(iconBoundsRect.width(), mIconSize) + paddingStart + paddingEnd
        val height = textBoundsRect.height().coerceAtLeast(Integer.max(iconBoundsRect.height(), mIconSize)) + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }
}