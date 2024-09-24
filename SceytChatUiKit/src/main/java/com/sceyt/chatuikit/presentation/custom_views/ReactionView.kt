package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.processEmojiCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class ReactionView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private lateinit var countTextBoundsRect: Rect
    private lateinit var smileTextPaint: TextPaint
    private lateinit var countTextPaint: TextPaint
    private lateinit var strokePaint: Paint
    private var countMargin = 0
    private var innerPadding = 0
    private var innerPaddingVertical = 0
    private var innerPaddingHorizontal = 0
    private var strokeColor = "#CDCDCF".toColorInt()
    private var countTetColor = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor)
    private var strikeWidth = 0
    private var cornerRadius = 30
    private var smileTextSize = 40
    private var countTextSize = 30
    private var countTitle = ""
    private var mCountMargin = 0
    private var reactionBackgroundColor: Int = 0
    private var counterTextMinWidth = 0
    private var enableStroke: Boolean = false
    private lateinit var smileStaticLayout: StaticLayout
    private var smileTitle: CharSequence = ""
        set(value) {
            field = if (isInEditMode)
                value
            else value.processEmojiCompat() ?: value
        }

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ReactionView)
            innerPadding = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionInnerPadding, 0)
            if (innerPadding == 0) {
                innerPaddingVertical = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionInnerPaddingVertical, 0)
                innerPaddingHorizontal = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionInnerPaddingHorizontal, 0)
            }
            reactionBackgroundColor = a.getColor(R.styleable.ReactionView_sceytUiReactionBackgroundColor, reactionBackgroundColor)
            countMargin = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionCountTextMargin, countMargin)
            smileTextSize = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionSmileTextSize, smileTextSize)
            countTextSize = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionCountTextSize, countTextSize)
            countTetColor = a.getColor(R.styleable.ReactionView_sceytUiReactionCountTextColor, countTetColor)
            strokeColor = a.getColor(R.styleable.ReactionView_sceytUiReactionStrokeColor, strokeColor)
            enableStroke = a.getBoolean(R.styleable.ReactionView_sceytUiReactionEnableStroke, enableStroke)
            if (enableStroke)
                strikeWidth = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionStrokeWidth, 0)
            cornerRadius = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionStrokeCornerRadius, cornerRadius)
            smileTitle = a.getString(R.styleable.ReactionView_sceytUiReactionSmileText)
                    ?: smileTitle
            countTitle = a.getString(R.styleable.ReactionView_sceytUiReactionCountText)
                    ?: countTitle
            counterTextMinWidth = a.getDimensionPixelSize(R.styleable.ReactionView_sceytUiReactionCountTextMinWidth, 0)
            a.recycle()
        }
        init()
    }

    private fun init() {
        smileTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = smileTextSize.toFloat()
        }

        countTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = countTextSize.toFloat()
            color = countTetColor
        }

        strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strikeWidth.toFloat()
            color = strokeColor
        }

        smileStaticLayout = getStaticLayout(smileTitle)
        countTextBoundsRect = Rect()
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

        var toCenterX = 0
        var toCenterY = 0

        val size = getSize()

        if (width > size.width) {
            val diff = (width - getSize().width) / 2
            toCenterX = diff
        }

        if (height > size.height) {
            val diff = (height - getSize().height) / 2
            toCenterY = diff
        }

        if (smileTitle.isNotBlank()) {
            canvas.save()
            canvas.translate((strikeWidth + getInnerPaddingHorizontal() + toCenterX).toFloat(),
                (height - smileStaticLayout.height) / 2f)
            smileStaticLayout.draw(canvas)
            canvas.restore()
        }

        if (countTitle.isNotBlank()) {
            val diff = counterTextMinWidth - countTextBoundsRect.width()
            val countDiffX = if (counterTextMinWidth > 0 && diff > 0) {
                diff / 2
            } else 0
            canvas.drawText(countTitle,
                (countDiffX - countTextBoundsRect.left + smileStaticLayout.width + mCountMargin + getInnerPaddingHorizontal()).toFloat() + toCenterX,
                abs(countTextBoundsRect.top) + getTopFormCountText() + getInnerPaddingVertical() + strikeWidth + toCenterY,
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
        return if (countTextBoundsRect.height() < smileStaticLayout.height) {
            (smileStaticLayout.height - countTextBoundsRect.height()) / 2f
        } else 0f
    }

    @Suppress("UNUSED")
    private fun getTopFormSmileText(): Float {
        return if (countTextBoundsRect.height() > smileStaticLayout.height) {
            (countTextBoundsRect.height() - smileStaticLayout.height) / 2f
        } else 0f
    }

    fun setReactionBackgroundColor(@ColorInt color: Int) {
        reactionBackgroundColor = color
        invalidate()
    }

    @Suppress("UNUSED")
    fun setReactionStrokeColor(@ColorInt color: Int) {
        strokeColor = color
        strokePaint.color = color
        invalidate()
    }

    @Suppress("UNUSED")
    fun setReactionBgAndStrokeColor(@ColorInt bgColor: Int, @ColorInt colorStroke: Int) {
        reactionBackgroundColor = bgColor
        strokeColor = colorStroke
        strokePaint.color = colorStroke
        invalidate()
    }

    fun setCountTextColor(@ColorInt color: Int) {
        countTetColor = color
        countTextPaint.color = color
        invalidate()
    }

    @Suppress("UNUSED")
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

    fun setSmileText(smileText: String, disableCount: Boolean) {
        smileTitle = smileText
        if (disableCount) countTitle = ""
        init()
        requestLayout()
    }

    fun setCountAndSmile(count: Number, smileText: String) {
        countTitle = count.toString()
        smileTitle = smileText
        init()
        requestLayout()
    }

    private fun getSize(): Size {
        val countTextWidth = if (countTitle.isBlank()) 0 else max(countTextBoundsRect.width(), counterTextMinWidth)
        val width = 2 * getInnerPaddingHorizontal() + smileStaticLayout.width + countTextWidth + mCountMargin + 2 * strikeWidth
        val height = 2 * getInnerPaddingVertical() + (smileStaticLayout.height).coerceAtLeast(countTextBoundsRect.height()) + 2 * strikeWidth
        return Size(width, height)
    }

    @Suppress("DEPRECATION")
    private fun getStaticLayout(title: CharSequence): StaticLayout {
        val width = Layout.getDesiredWidth(title, smileTextPaint).toInt()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(title, 0, title.length, smileTextPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false).build()
        } else StaticLayout(title, smileTextPaint, width, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (countTitle.isBlank() && smileTitle.isBlank()) {
            setMeasuredDimension(0, 0)
            return
        }

        val size = getSize()
        val desiredWidth = size.width
        val desiredHeight = size.height

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        //Measure Width
        val width: Int = when (widthMode) {
            //Must be this size
            MeasureSpec.EXACTLY -> max(widthSize, desiredWidth)
            //Can't be bigger than...
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            //Be whatever you want
            else -> desiredWidth
        }

        //Measure Height
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> max(heightSize, desiredHeight)
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}