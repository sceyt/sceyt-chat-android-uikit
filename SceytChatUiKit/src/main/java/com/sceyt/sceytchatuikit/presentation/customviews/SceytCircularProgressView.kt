package com.sceyt.sceytchatuikit.presentation.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPxAsFloat
import com.sceyt.sceytchatuikit.extensions.inNotNanOrZero
import com.sceyt.sceytchatuikit.extensions.scaleAndAlphaAnim
import kotlin.math.max
import kotlin.math.min

class SceytCircularProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private lateinit var progressPaint: Paint
    private lateinit var trackPaint: Paint
    private lateinit var backgroundPaint: Paint
    private var rotateAnim: ValueAnimator? = null
    private var updateProgressAnim: ValueAnimator? = null

    private var iconSize = 0
    private val rect = RectF()
    private val rectBg = RectF()
    private var startAngle = -90f
    private val maxAngle = 360f
    private val maxProgress = 100f

    private var diameter = 0f
    private var angle = 0f
    private var progress = 0f
    private var minProgress = 0f
    private var centerIcon: Drawable? = null
    private var enableTrack = true
    private var rotateAnimEnabled = true
    private var enableProgressDownAnimation = false
    private var trackThickness = dpToPxAsFloat(3.2f)
    private var roundedProgress = true
    private var trackColor = "#1A21CFB9".toColorInt()
    private var progressColor = "#17BCA7".toColorInt()
    private var iconTintColor: Int = Color.WHITE
    private var bgColor: Int = 0
    private var iconHeight: Int = 0
    private var iconWidth: Int = 0
    private var iconSizeInPercent: Float = 50f
    private var transferring: Boolean = true
    private var animatingToAngle = angle
    private var drawingProgressAnimEndCb: ((Boolean) -> Unit)? = null
    private var visibleAnim: AnimationSet? = null
    private var goneAnim: AnimationSet? = null

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytCircularProgressView)
            progressColor = a.getColor(R.styleable.SceytCircularProgressView_progressColor, progressColor)
            trackColor = a.getColor(R.styleable.SceytCircularProgressView_trackColor, trackColor)
            minProgress = a.getFloat(R.styleable.SceytCircularProgressView_minProgress, minProgress)
            progress = a.getFloat(R.styleable.SceytCircularProgressView_progress, minProgress)
            roundedProgress = a.getBoolean(R.styleable.SceytCircularProgressView_roundedProgress, roundedProgress)
            centerIcon = a.getDrawable(R.styleable.SceytCircularProgressView_centerIcon)
            rotateAnimEnabled = a.getBoolean(R.styleable.SceytCircularProgressView_rotateAnimEnabled, rotateAnimEnabled)
            enableProgressDownAnimation = a.getBoolean(R.styleable.SceytCircularProgressView_enableProgressDownAnimation, enableProgressDownAnimation)
            iconTintColor = a.getColor(R.styleable.SceytCircularProgressView_iconTint, iconTintColor)
            bgColor = a.getColor(R.styleable.SceytCircularProgressView_backgroundColor, 0)
            iconSizeInPercent = getNormalizedPercent(a.getFloat(R.styleable.SceytCircularProgressView_iconSizeInPercent, iconSizeInPercent))
            val trackThickness = a.getDimensionPixelSize(R.styleable.SceytCircularProgressView_trackThickness, 0)
            if (trackThickness > 0)
                this.trackThickness = trackThickness.toFloat()
            a.recycle()
        }

        init()
    }

    private fun init() {
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = trackThickness
            color = progressColor
            strokeCap = if (roundedProgress) Paint.Cap.ROUND else Paint.Cap.BUTT
        }

        trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = trackThickness
            color = trackColor
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }

        startAngle = if (rotateAnimEnabled) 0f else -90f
        angle = calculateAngle(progress)

        post {
            rotate()
            initCenterIcon()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (bgColor != 0)
            canvas.drawArc(rectBg, 0f, 360f, false, backgroundPaint)

        if (transferring) {
            //Draw track
            if (enableTrack)
                drawCircle(0f, maxAngle, canvas, trackPaint)
            //Draw progress
            drawCircle(startAngle, angle, canvas, progressPaint)
        }
        //Draw icon
        centerIcon?.let {
            if (isInEditMode) initCenterIcon()
            val left = (width - iconWidth) / 2
            val top = (height - iconHeight) / 2
            it.setBounds(left, top, left + iconWidth, top + iconHeight)
            it.draw(canvas)
        }
    }

    private fun rotate() {
        if (rotateAnimEnabled && transferring && (rotateAnim == null || rotateAnim?.isRunning != true)) {
            rotateAnim?.cancel()
            rotateAnim = ValueAnimator.ofFloat(0f, 360f).apply {
                addUpdateListener { animation ->
                    startAngle = (animation.animatedValue as Float)

                    if (startAngle > 360)
                        startAngle = 0f

                    if (angle >= 360f || angle == 0f)
                        rotateAnim?.cancel()

                    invalidate()
                    duration = 2000
                    repeatCount = Animation.INFINITE
                    interpolator = LinearInterpolator()
                }
                start()
            }
        }
    }

    private fun drawProgress(newAngel: Float) {
        if (newAngel != angle && !checkIdProgressDownAndDraw(newAngel)) {
            animatingToAngle = newAngel
            if ((updateProgressAnim == null || updateProgressAnim?.isRunning != true)) {
                updateProgressAnim = ValueAnimator.ofFloat(angle, newAngel).apply {
                    addUpdateListener { animation ->
                        this@SceytCircularProgressView.angle = (animation.animatedValue as Float)
                        if (rotateAnimEnabled.not())
                            invalidate()
                    }
                    duration = 300
                    interpolator = LinearInterpolator()
                    start()

                    doOnEnd {
                        if (animatingToAngle != angle) {
                            drawProgress(animatingToAngle)
                        } else drawingProgressAnimEndCb?.invoke(true)
                    }
                }
            }
        }
        rotate()
    }

    private fun checkIdProgressDownAndDraw(newAngel: Float): Boolean {
        if (!enableProgressDownAnimation && newAngel < angle) {
            updateProgressAnim?.cancel()
            angle = newAngel
            invalidate()
            return true
        }
        return false
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        diameter = width.coerceAtMost(height).toFloat()
        updateRect()
        initCenterIcon()
    }

    private fun updateRect() {
        val strokeWidth = trackPaint.strokeWidth
        rect.set(strokeWidth + paddingStart, strokeWidth + paddingTop, diameter - strokeWidth - paddingEnd, diameter - strokeWidth - paddingBottom)
        rectBg.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    private fun drawCircle(startAngle: Float, angle: Float, canvas: Canvas, paint: Paint) {
        canvas.drawArc(rect, startAngle, angle, false, paint)
    }

    private fun calculateAngle(progress: Float) = maxAngle / maxProgress * progress

    private fun getNormalizedPercent(percent: Float): Float {
        return if (percent <= 0 || percent > 100)
            20f else percent
    }

    private fun initCenterIcon() {
        val icon = centerIcon ?: return
        initIconSize()
        if (icon.intrinsicWidth > icon.intrinsicHeight) {
            iconWidth = min(iconSize, Integer.max(iconSize, icon.intrinsicWidth))
            iconHeight = iconWidth * icon.intrinsicHeight / icon.intrinsicWidth
        } else {
            iconHeight = min(iconSize, Integer.max(iconSize, icon.intrinsicHeight))
            iconWidth = iconHeight * icon.intrinsicWidth / icon.intrinsicHeight
        }

        if (iconTintColor != 0)
            icon.setTint(iconTintColor)
    }

    private fun initIconSize() {
        if (iconSize <= 0)
            iconSize = (width * iconSizeInPercent / 100 - paddingStart - paddingEnd).toInt()
    }

    fun release(withProgress: Float? = null) {
        progress = max(withProgress?.inNotNanOrZero() ?: 0f, minProgress)
        if (progress.isNaN()) progress = 0f
        angle = calculateAngle(progress)
        transferring = true
        if (rotateAnimEnabled) rotate()
        invalidate()
    }

    fun setProgress(@FloatRange(from = 0.0, to = 100.0) newProgress: Float) {
        progress = max(newProgress.inNotNanOrZero(), minProgress)
        transferring = true
        drawProgress(calculateAngle(progress))
    }

    fun setProgressColor(color: Int) {
        progressPaint.color = color
        progressColor = color
        invalidate()
    }

    fun setTrackColor(color: Int) {
        trackPaint.color = color
        trackColor = color
        invalidate()
    }

    fun setThickness(width: Float) {
        progressPaint.strokeWidth = width
        trackPaint.strokeWidth = width
        updateRect()
        invalidate()
    }

    fun setRotateAnimEnabled(enabled: Boolean) {
        rotateAnimEnabled = enabled
        if (!enabled)
            rotateAnim?.cancel()
        invalidate()
    }

    fun setRounded(rounded: Boolean) {
        progressPaint.strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT
        invalidate()
    }

    fun setIcon(drawable: Drawable?) {
        centerIcon = drawable
        invalidate()
    }

    fun setTransferring(transferring: Boolean) {
        this.transferring = transferring
        invalidate()
    }

    fun hideAwaitToAnimFinish(hideCb: ((Boolean) -> Unit)? = null) {
        if (updateProgressAnim?.isRunning == true) {
            drawingProgressAnimEndCb = {
                isVisible = false
                hideCb?.invoke(true)
            }
        } else {
            hideCb?.invoke(true)
            isVisible = false
        }
    }

    fun getProgressAnim() = updateProgressAnim

    private fun setVisibleWithAnim() {
        goneAnim?.cancel()
        if (visibleAnim == null || visibleAnim?.hasStarted() != true || visibleAnim?.hasEnded() == true) {
            if (!isVisible) {
                super.setVisibility(VISIBLE)
                visibleAnim = scaleAndAlphaAnim(0.5f, 1f, duration = 100)
            }
        }
    }

    private fun setGoneWithAnim() {
        visibleAnim?.cancel()
        if (goneAnim == null || goneAnim?.hasStarted() != true || goneAnim?.hasEnded() == true) {
            if (isVisible) {
                goneAnim = scaleAndAlphaAnim(1f, 0.5f, duration = 100) {
                    super.setVisibility(GONE)
                }
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        if (isAttachedToWindow) {
            if (visibility == VISIBLE)
                setVisibleWithAnim()
            else setGoneWithAnim()
        } else {
            super.setVisibility(visibility)
            drawingProgressAnimEndCb = null
        }
    }

    override fun setBackgroundColor(color: Int) {
        bgColor = color
        backgroundPaint.color = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val requestedWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val desiredWidth = 150

        val width = when (requestedWidthMode) {
            MeasureSpec.EXACTLY -> requestedWidth
            MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> requestedWidth.coerceAtMost(desiredWidth)
        }

        setMeasuredDimension(width, width)
    }
}