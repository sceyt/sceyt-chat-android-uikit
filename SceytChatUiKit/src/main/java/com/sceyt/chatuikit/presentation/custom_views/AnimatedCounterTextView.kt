package com.sceyt.chatuikit.presentation.custom_views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.styles.common.TextStyle
import kotlin.math.max
import androidx.core.graphics.withSave

/**
 * Extension function to apply TextStyle directly to AnimatedCounterTextView
 */
fun TextStyle.apply(view: AnimatedCounterTextView) {
    view.applyTextStyle(this)
}

/**
 * Custom View that animates count changes with a sliding up/down effect.
 * When the count increases, old text slides up and new text slides up from below.
 * When the count decreases, old text slides down and new text slides down from above.
 */
class AnimatedCounterTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentText: String = ""
    private var nextText: String = ""
    private var animationProgress = 0f
    private var isAnimating = false
    private var isIncreasing = true

    private val textBounds = Rect()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private var animator: ValueAnimator? = null

    companion object {
        private const val ANIMATION_DURATION = 300L
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.AnimatedCounterTextView)
            .use { typedArray ->
                // Text color
                val textColor = typedArray.getColor(
                    R.styleable.AnimatedCounterTextView_android_textColor,
                    context.getColor(R.color.sceyt_color_text_primary)
                )
                textPaint.color = textColor

                // Text size
                val textSize = typedArray.getDimensionPixelSize(
                    R.styleable.AnimatedCounterTextView_android_textSize,
                    context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
                )
                textPaint.textSize = textSize.toFloat()

                // Font family
                val fontResId = typedArray.getResourceId(
                    R.styleable.AnimatedCounterTextView_android_fontFamily,
                    -1
                )
                if (fontResId != -1) {
                    val typeface = ResourcesCompat.getFont(context, fontResId)
                    textPaint.typeface = typeface
                }

                // Initial text
                val initialText =
                    typedArray.getString(R.styleable.AnimatedCounterTextView_android_text)

                currentText = initialText.orEmpty()
            }
    }

    /**
     * Sets text color
     */
    fun setTextColor(color: Int) {
        textPaint.color = color
        if (!isAnimating) {
            invalidate()
        }
    }

    /**
     * Sets text size in pixels
     */
    fun setTextSize(size: Float) {
        textPaint.textSize = size
        requestLayout()
        invalidate()
    }

    /**
     * Sets typeface
     */
    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
        requestLayout()
        invalidate()
    }

    /**
     * Gets current text
     */
    fun getText(): String = currentText

    /**
     * Sets the text with animation if enabled
     */
    fun setTextAnimated(newText: String, animate: Boolean = true) {
        // Cancel any ongoing animation
        animator?.cancel()

        if (!animate || currentText.isEmpty()) {
            currentText = newText
            requestLayout()
            invalidate()
            return
        }

        // Determine direction based on numeric comparison if possible
        isIncreasing = try {
            val currentNum = currentText.toIntOrNull() ?: 0
            val newNum = newText.toIntOrNull() ?: 0
            newNum > currentNum
        } catch (_: Exception) {
            true
        }

        nextText = newText
        startAnimation()
    }

    /**
     * Sets text without animation
     */
    fun setText(text: String) {
        setTextAnimated(text, animate = false)
    }

    /**
     * Applies TextStyle to this view
     */
    fun applyTextStyle(textStyle: TextStyle) {
        textStyle.apply(context, textPaint)
        requestLayout()
        invalidate()
    }

    private fun startAnimation() {
        isAnimating = true
        requestLayout()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    currentText = nextText
                    animationProgress = 0f
                    invalidate()
                }
            })

            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textToMeasure = nextText.takeIf { it.isNotBlank() } ?: currentText

        textPaint.getTextBounds(textToMeasure, 0, textToMeasure.length, textBounds)

        val desiredWidth = textBounds.width() + paddingLeft + paddingRight
        // Use single text height - animation will draw beyond bounds (borderless)
        val desiredHeight = textBounds.height() + paddingTop + paddingBottom

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(max(minimumWidth, width), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Save canvas state and disable clipping for borderless animation
        canvas.withSave {
            if (!isAnimating) {
                // Draw static text
                drawStaticText(canvas, currentText)
                canvas.restoreToCount(saveCount)
                return
            }

            // Calculate positions
            val centerY = height / 2f
            val textHeight = getTextHeight(currentText)

            val currentAlpha = ((1f - animationProgress) * 255).toInt()
            val nextAlpha = (animationProgress * 255).toInt()

            // Calculate translation based on direction
            val translation = if (isIncreasing) {
                // Increasing: slide up
                -textHeight * animationProgress
            } else {
                // Decreasing: slide down
                textHeight * animationProgress
            }

            // Draw current text (moving out)
            if (currentAlpha > 0) {
                textPaint.alpha = currentAlpha
                val currentY = centerY + getTextBaseline(currentText) + translation
                drawCenteredText(canvas, currentText, currentY)
            }

            // Draw next text (moving in)
            if (nextAlpha > 0) {
                textPaint.alpha = nextAlpha
                val nextStartOffset = if (isIncreasing) textHeight else -textHeight
                val nextY = centerY + getTextBaseline(nextText) + nextStartOffset + translation
                drawCenteredText(canvas, nextText, nextY)
            }

            // Restore alpha and canvas
            textPaint.alpha = 255
        }
    }

    private fun drawStaticText(canvas: Canvas, text: String) {
        if (text.isEmpty()) return

        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val x = (width - textBounds.width()) / 2f - textBounds.left
        val y = height / 2f - textBounds.exactCenterY()

        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, y: Float) {
        if (text.isEmpty()) return

        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val x = (width - textBounds.width()) / 2f - textBounds.left
        canvas.drawText(text, x, y, textPaint)
    }

    private fun getTextHeight(text: String): Float {
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        return textBounds.height().toFloat()
    }

    private fun getTextBaseline(text: String): Float {
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        return -textBounds.exactCenterY()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }
}
