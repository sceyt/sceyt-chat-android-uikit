package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationSet
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.scaleAndAlphaAnim

class SceytOnlineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var strokeColor = Color.BLACK
    private var indicatorColor = Color.GREEN
    private var changeVisibilityWithAnim = true
    private var strokeWidth = 0
    private var visibleAnim: AnimationSet? = null
    private var goneAnim: AnimationSet? = null

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytOnlineView)
            strokeColor = a.getColor(R.styleable.SceytOnlineView_sceytOnlineViewStrokeColor, strokeColor)
            indicatorColor = a.getColor(R.styleable.SceytOnlineView_sceytOnlineViewIndicatorColor, indicatorColor)
            strokeWidth = a.getDimensionPixelSize(R.styleable.SceytOnlineView_sceytOnlineViewStrokeWidth, strokeWidth)
            changeVisibilityWithAnim = a.getBoolean(R.styleable.SceytOnlineView_sceytOnlineViewChangeVisibilityWithAnim, changeVisibilityWithAnim)
            a.recycle()
        }
        init()
    }

    private fun init() {
        with(strokePaint) {
            flags = Paint.ANTI_ALIAS_FLAG
            color = strokeColor
            strokeWidth = this@SceytOnlineView.strokeWidth.toFloat()
            style = Paint.Style.STROKE
        }
        with(indicatorPaint) {
            flags = Paint.ANTI_ALIAS_FLAG
            color = indicatorColor
            style = Paint.Style.FILL
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val halfStrokeW = strokeWidth / 2
        val smallDiff = 1
        if (strokeWidth > 0)
            canvas.drawCircle(width / 2f, height / 2f, width / 2f - halfStrokeW - smallDiff, strokePaint)
        canvas.drawCircle(width / 2f, height / 2f, width / 2f - strokeWidth - smallDiff, indicatorPaint)
    }

    override fun setVisibility(visibility: Int) {
        if (changeVisibilityWithAnim && isAttachedToWindow) {
            if (visibility == VISIBLE)
                setVisibleWithAnim()
            else setGoneWithAnim()
        } else
            super.setVisibility(visibility)
    }

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

    fun setStrokeColor(color: Int) {
        strokeColor = color
        init()
        invalidate()
    }

    fun setIndicatorColor(color: Int) {
        indicatorColor = color
        init()
        invalidate()
    }

    fun changeVisibilityWithAnimation(withAnim: Boolean) {
        changeVisibilityWithAnim = withAnim
    }
}