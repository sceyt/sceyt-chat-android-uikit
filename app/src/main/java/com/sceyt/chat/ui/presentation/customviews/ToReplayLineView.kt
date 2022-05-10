package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.sceyt.chat.ui.R

class ToReplayLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private var path = Path()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokeWidth = 5f
    private var isToLeft = false
    private var connectedViewHalfHeight = 0
    private var connectedViewId: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ToReplayLineView)
        isToLeft = a.getBoolean(R.styleable.ToReplayLineView_toLeft, false)
        connectedViewId = a.getResourceId(R.styleable.ToReplayLineView_connectedView, 0)
        //val view = rootView.findViewById<View>(connectedView)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (height - width < 0) connectedViewHalfHeight = 0
        if (isToLeft)
            drawToLeft(canvas)
        else drawToRight(canvas)
    }

    private fun initPaint() {
        paint.color = "#CDCDCF".toColorInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
    }

    private fun drawToRight(canvas: Canvas) {
        path.reset()
        initPaint()

        path.moveTo(strokeWidth / 2, 0f)
        path.lineTo(strokeWidth / 2, (height - width).toFloat())

        if (width < height) {
            path.lineTo(strokeWidth / 2, (height - width).toFloat())
            path.cubicTo(strokeWidth / 2, (height - width).toFloat(), strokeWidth / 2, height.toFloat(),
                width.toFloat(), height - strokeWidth / 2)
        } else {
            path.lineTo(strokeWidth / 2, 0f)
            path.cubicTo(strokeWidth / 2, 0f, strokeWidth / 2, height.toFloat(),
                width.toFloat(), height - strokeWidth / 2)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawToLeft(canvas: Canvas) {
        path.reset()
        initPaint()

        path.moveTo(width - strokeWidth / 2, 0f)

        if (width < height) {
            path.lineTo(width - strokeWidth / 2, (height - width).toFloat())
            path.cubicTo(width - strokeWidth / 2, (height - width).toFloat(), width.toFloat(), height.toFloat(),
                0f, height - strokeWidth / 2)
        } else {
            path.lineTo(width - strokeWidth / 2, 0f)
            path.cubicTo(width - strokeWidth / 2, 0f, width.toFloat(), height.toFloat(),
                0f, height - strokeWidth / 2)
        }
        canvas.drawPath(path, paint)
    }
}