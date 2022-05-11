package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
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
    private var textColor = Color.BLACK
    private var statusDrawable: Drawable? = null
    private var firstStatusIcon = true
    private var mMargin = 0
    private var mIconSize = 0
    private var isHighlighted = false

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
            a.recycle()
        }
        init()
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

        statusDrawable?.let {
            initStatsIconSize(it)

            val left = textBoundsRect.right + mMargin - textBoundsRect.left + paddingStart
            val top = getTopFormIcon() + paddingTop
            iconBoundsRect.set(left, top, left + mIconSize, top + heightIcon)
        }
    }

    private fun measureViewsFirstStatus() {
        textPaint.getTextBounds(dateText, 0, dateText.length, textBoundsRect)

        statusDrawable?.let {
            initStatsIconSize(it)

            val left = paddingStart
            val top = getTopFormIcon() + paddingTop
            iconBoundsRect.set(left, top, left + mIconSize, top + heightIcon)
        }
    }

    private fun checkSizesAndMargins() {
        if (statusDrawable == null)
            mIconSize = 0

        if (statusDrawable == null || dateText.isBlank())
            mMargin = 0
    }

    private fun initStatsIconSize(icon: Drawable) {
        mIconSize = if (mIconSize == 0 && textBoundsRect.height() != 0)
            textBoundsRect.height() * icon.intrinsicWidth / icon.intrinsicHeight
        else icon.intrinsicWidth

        heightIcon = mIconSize * icon.intrinsicHeight / icon.intrinsicWidth
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
                -textBoundsRect.left.toFloat() + iconBoundsRect.right + mMargin,
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
        return if (textBoundsRect.height() < heightIcon)
            (heightIcon - (textBoundsRect.top.absoluteValue)) / 2
        else 0
    }

    /** For center vertical status icon when text height is bigger*/
    private fun getTopFormIcon(): Int {
        return if (heightIcon < textBoundsRect.height()) {
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
        }
    }

    fun setStatusIcon(drawable: Drawable?) {
        statusDrawable = drawable
        init()
        requestLayout()
    }

    fun setDateText(text: String) {
        dateText = text
        init()
        requestLayout()
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
        val width = textBoundsRect.width() + mMargin + iconBoundsRect.width() + paddingStart + paddingEnd
        val height = textBoundsRect.height().coerceAtLeast(heightIcon) + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }
}