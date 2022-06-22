package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.sceyt.chat.ui.R

class SceytOnlineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var strokeColor = Color.BLACK
    private var indicatorColor = Color.GREEN
    private var strokeWidth = 0

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytOnlineView)
            strokeColor = a.getColor(R.styleable.SceytOnlineView_sceytOnlineViewStrokeColor, strokeColor)
            indicatorColor = a.getColor(R.styleable.SceytOnlineView_sceytOnlineViewIndicatorColor, indicatorColor)
            strokeWidth = a.getDimensionPixelSize(R.styleable.SceytOnlineView_sceytOnlineViewStrokeWidth, strokeWidth)
            a.recycle()
        }
        init()
    }

    private fun init() {
        with(strokePaint) {
            color = strokeColor
            strokeWidth = this@SceytOnlineView.strokeWidth.toFloat()
            style = Paint.Style.STROKE
        }
        with(indicatorPaint) {
            color = indicatorColor
            style = Paint.Style.FILL
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        canvas.drawCircle(width / 2f, height / 2f, width / 2f - strokeWidth / 2, strokePaint)
        canvas.drawCircle(width / 2f, height / 2f, width / 2f - strokeWidth, indicatorPaint)
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
}