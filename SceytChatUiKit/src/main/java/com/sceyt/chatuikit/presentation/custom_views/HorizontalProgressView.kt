package com.sceyt.chatuikit.presentation.custom_views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R

class HorizontalProgressView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var cornerRadius = 0f
    private var currentProgress = 0f
    private var progressAnimator: ValueAnimator? = null

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            currentProgress = field.toFloat()
            invalidate()
        }

    @setparam:ColorInt
    var progressColor: Int
        get() = progressPaint.color
        set(value) {
            progressPaint.color = value
            invalidate()
        }

    @setparam:ColorInt
    var progressTrackColor: Int
        get() = backgroundPaint.color
        set(value) {
            backgroundPaint.color = value
            invalidate()
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.HorizontalProgressView).use { typedArray ->
            val defaultBackgroundColor = 0xFFD0D8E3.toInt()
            val defaultProgressColor = context.getColor(R.color.sceyt_color_accent)

            progressTrackColor = typedArray.getColor(
                R.styleable.HorizontalProgressView_horizontalProgressViewTrackColor,
                defaultBackgroundColor
            )
            progressColor = typedArray.getColor(
                R.styleable.HorizontalProgressView_horizontalProgressViewColor,
                defaultProgressColor
            )
            progress = typedArray.getInt(R.styleable.HorizontalProgressView_horizontalProgressViewProgress, 0)
            cornerRadius = typedArray.getDimension(
                R.styleable.HorizontalProgressView_horizontalProgressViewCornerRadius,
                8f * resources.displayMetrics.density
            )
        }

        currentProgress = progress.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw background
        rectF.set(0f, 0f, width, height)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)

        // Draw progress
        if (currentProgress > 0) {
            val progressWidth = width * (currentProgress / 100f)
            rectF.set(0f, 0f, progressWidth, height)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, progressPaint)
        }
    }

    fun setProgress(value: Int, animate: Boolean = false, duration: Long = 200) {
        if (animate) {
            animateProgress(currentProgress.toInt(), value, duration)
        } else {
            cancelAnimation()
            progress = value
        }
    }

    fun cancelAnimation() {
        progressAnimator?.cancel()
    }

    private fun animateProgress(from: Int, to: Int, duration: Long = 200) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimation()
    }
}

