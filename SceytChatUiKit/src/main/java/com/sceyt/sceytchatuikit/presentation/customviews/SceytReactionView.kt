package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.sceyt.sceytchatuikit.R
import kotlin.math.abs
import kotlin.math.max


class SceytReactionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private lateinit var smileTextBoundsRect: Rect
    private lateinit var countTextBoundsRect: Rect
    private lateinit var smileTextPaint: Paint
    private lateinit var countTextPaint: Paint
    private lateinit var strokePaint: Paint
    private var countMargin = 0
    private var innerPadding = 0
    private var innerPaddingVertical = 0
    private var innerPaddingHorizontal = 0
    private var strokeColor = "#CDCDCF".toColorInt()
    private var countTetColor = Color.BLACK
    private var strikeWidth = 0
    private var cornerRadius = 30
    private var smileTextSize = 40
    private var countTextSize = 30
    private var smileTitle = ""
    private var countTitle = ""
    private var mCountMargin = 0
    private var reactionBackgroundColor: Int = 0
    private var counterTextMinWidth = 0
    private var enableStroke: Boolean = true

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytReactionView)
            innerPadding = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewInnerPadding, 0)
            if (innerPadding == 0) {
                innerPaddingVertical = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewInnerPaddingVertical, 0)
                innerPaddingHorizontal = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewInnerPaddingHorizontal, 0)
            }
            reactionBackgroundColor = a.getColor(R.styleable.SceytReactionView_sceytReactionViewBackgroundColor, reactionBackgroundColor)
            countMargin = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewCountTextMargin, countMargin)
            smileTextSize = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewSmileTextSize, smileTextSize)
            countTextSize = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewCountTextSize, countTextSize)
            countTetColor = a.getColor(R.styleable.SceytReactionView_sceytReactionViewCountTextColor, countTetColor)
            strokeColor = a.getColor(R.styleable.SceytReactionView_sceytReactionViewStrokeColor, strokeColor)
            enableStroke = a.getBoolean(R.styleable.SceytReactionView_sceytReactionViewEnableStroke, enableStroke)
            if (enableStroke)
                strikeWidth = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewStrokeWidth, 0)
            cornerRadius = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewStrokeCornerRadius, cornerRadius)
            smileTitle = a.getString(R.styleable.SceytReactionView_sceytReactionViewSmileText)
                    ?: smileTitle
            countTitle = a.getString(R.styleable.SceytReactionView_sceytReactionViewCountText)
                    ?: countTitle
            counterTextMinWidth = a.getDimensionPixelSize(R.styleable.SceytReactionView_sceytReactionViewCountTextMinWidth, 0)
            a.recycle()
        }
        init()
    }

    private fun init() {
        smileTextBoundsRect = Rect()
        countTextBoundsRect = Rect()
        smileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = smileTextSize.toFloat()
        }

        countTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = countTextSize.toFloat()
            color = countTetColor
        }

        strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strikeWidth.toFloat()
            color = strokeColor
        }

        smileTextPaint.getTextBounds(smileTitle, 0, smileTitle.length, smileTextBoundsRect)
        countTextPaint.getTextBounds(countTitle, 0, countTitle.length, countTextBoundsRect)

        mCountMargin = if (countTitle.isBlank()) {
            0
        } else countMargin
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (countTitle.isBlank() && smileTitle.isBlank()) return

        val rectF = RectF(strikeWidth.toFloat(), strikeWidth.toFloat(), (width - strikeWidth).toFloat(), (height - strikeWidth).toFloat())
        canvas.drawRoundRect(rectF, cornerRadius.toFloat(), cornerRadius.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = reactionBackgroundColor
            style = Paint.Style.FILL
        })

        if (smileTitle.isNotBlank()) {
            canvas.drawText(smileTitle,
                -smileTextBoundsRect.left.toFloat() + strikeWidth + getInnerPaddingHorizontal(),
                abs(smileTextBoundsRect.top) + getTopFormSmileText() + strikeWidth + getInnerPaddingVertical(),
                smileTextPaint)
        }

        if (countTitle.isNotBlank()) {
            val diff = counterTextMinWidth - countTextBoundsRect.width()
            val countDiffX = if (counterTextMinWidth > 0 && diff > 0) {
                diff / 2
            } else 0
            canvas.drawText(countTitle,
                (countDiffX - countTextBoundsRect.left + smileTextBoundsRect.right + mCountMargin + getInnerPaddingHorizontal()).toFloat(),
                abs(countTextBoundsRect.top) + getTopFormCountText() + getInnerPaddingVertical() + strikeWidth,
                countTextPaint)
        }

        if (enableStroke)
            canvas.drawRoundRect(rectF, cornerRadius.toFloat(), cornerRadius.toFloat(), strokePaint)
    }

    private fun getInnerPaddingVertical(): Int {
        return if (innerPadding > 0) innerPadding
        else innerPaddingVertical
    }

    private fun getInnerPaddingHorizontal(): Int {
        return if (innerPadding > 0) innerPadding
        else innerPaddingHorizontal
    }

    private fun getTopFormCountText(): Float {
        return if (countTextBoundsRect.height() < smileTextBoundsRect.height()) {
            (smileTextBoundsRect.height() - countTextBoundsRect.height()) / 2f
        } else 0f
    }

    private fun getTopFormSmileText(): Float {
        return if (countTextBoundsRect.height() > smileTextBoundsRect.height()) {
            (countTextBoundsRect.height() - smileTextBoundsRect.height()) / 2f
        } else 0f
    }

    fun setReactionBackgroundColor(@ColorInt color: Int) {
        reactionBackgroundColor = color
        invalidate()
    }

    fun setReactionStrokeColor(@ColorInt color: Int) {
        strokeColor = color
        strokePaint.color = color
        invalidate()
    }

    fun setReactionBgAndStrokeColor(@ColorInt bgColor: Int, @ColorInt colorStroke: Int) {
        reactionBackgroundColor = bgColor
        strokeColor = colorStroke
        strokePaint.color = colorStroke
        invalidate()
    }

    fun setCount(count: Number) {
        countTitle = count.toString()
        init()
        requestLayout()
    }

    fun setSmileText(smileText: String) {
        smileTitle = smileText
        init()
        requestLayout()
    }

    fun setCountAndSmile(count: Number, smileText: String) {
        countTitle = count.toString()
        smileTitle = smileText
        init()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (countTitle.isBlank() && smileTitle.isBlank()) {
            setMeasuredDimension(0, 0)
            return
        }

        val countTextWidth = if (countTitle.isBlank()) 0 else max(countTextBoundsRect.width(), counterTextMinWidth)
        val width = 2 * getInnerPaddingHorizontal() + smileTextBoundsRect.width() + countTextWidth + mCountMargin + 2 * strikeWidth
        val height = 2 * getInnerPaddingVertical() + smileTextBoundsRect.height().coerceAtLeast(countTextBoundsRect.height()) + 2 * strikeWidth
        setMeasuredDimension(width, height)
    }
}