package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.sceyt.chat.ui.R
import kotlin.math.abs


class ReactionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
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
    private var strikeWidth = 3
    private var cornerRadius = 30
    private var smileTextSize = 40
    private var countTextSize = 30
    private var smiteTitle = ""
    private var countTitle = ""
    private var mCountMargin = 0

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ReactionView)
            innerPadding = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewInnerPadding, innerPadding)
            if (innerPadding == 0) {
                innerPaddingVertical = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewInnerPaddingVertical, 0)
                innerPaddingHorizontal = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewInnerPaddingHorizontal, 0)
            }
            countMargin = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewCountTextMargin, countMargin)
            smileTextSize = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewSmileTextSize, smileTextSize)
            countTextSize = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewCountTextSize, countTextSize)
            countTetColor = a.getColor(R.styleable.ReactionView_reactionViewCountTextColor, countTetColor)
            strokeColor = a.getColor(R.styleable.ReactionView_reactionViewStrokeColor, strokeColor)
            strikeWidth = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewStrokeWidth, strikeWidth)
            cornerRadius = a.getDimensionPixelSize(R.styleable.ReactionView_reactionViewStrokeCornerRadius, cornerRadius)
            smiteTitle = a.getString(R.styleable.ReactionView_reactionViewSmileText) ?: smiteTitle
            countTitle = a.getString(R.styleable.ReactionView_reactionViewCountText) ?: countTitle
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

        smileTextPaint.getTextBounds(smiteTitle, 0, smiteTitle.length, smileTextBoundsRect)
        countTextPaint.getTextBounds(countTitle, 0, countTitle.length, countTextBoundsRect)

        mCountMargin = if (countTitle.isBlank()) {
            0
        } else countMargin
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (countTitle.isBlank() && smiteTitle.isBlank()) return

        if (smiteTitle.isNotBlank()) {
            canvas.drawText(smiteTitle,
                -smileTextBoundsRect.left.toFloat() + strikeWidth + getInnerPaddingHorizontal(),
                abs(smileTextBoundsRect.top) + getTopFormSmileText() + strikeWidth + getInnerPaddingVertical(),
                smileTextPaint)
        }

        if (countTitle.isNotBlank()) {
            canvas.drawText(countTitle,
                (-countTextBoundsRect.left + smileTextBoundsRect.right + mCountMargin + getInnerPaddingHorizontal()).toFloat(),
                abs(countTextBoundsRect.top) + getTopFormCountText() + getInnerPaddingVertical() + strikeWidth,
                countTextPaint)
        }

        canvas.drawRoundRect(RectF(strikeWidth.toFloat(), strikeWidth.toFloat(), (width - strikeWidth).toFloat(), (height - strikeWidth).toFloat()),
            cornerRadius.toFloat(), cornerRadius.toFloat(), strokePaint)
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

    fun setCount(count: Number) {
        countTitle = count.toString()
        init()
        requestLayout()
    }

    fun setSmileText(smileText: String) {
        smiteTitle = smileText
        init()
        requestLayout()
    }

    fun setCountAndSmile(count: Number, smileText: String) {
        countTitle = count.toString()
        smiteTitle = smileText
        init()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (countTitle.isBlank() && smiteTitle.isBlank()) {
            setMeasuredDimension(0, 0)
            return
        }
        val width = 2 * getInnerPaddingHorizontal() + smileTextBoundsRect.width() + countTextBoundsRect.width() + mCountMargin + 2 * strikeWidth
        val height = 2 * getInnerPaddingVertical() + smileTextBoundsRect.height().coerceAtLeast(countTextBoundsRect.height()) + 2 * strikeWidth
        setMeasuredDimension(width, height)
    }
}