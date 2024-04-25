package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPxAsFloat
import com.sceyt.chatuikit.extensions.getCompatColor

class SceytToReplyLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private var path = Path()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokeWidth = dpToPxAsFloat(1.8f)
    private var isToLeft = false
    private var strokeColor = context.getCompatColor(SceytChatUIKit.theme.borderColor)
    private var connectedViewTopId: Int
    private var connectedViewBottomId: Int
    private var connectedViewTopHalfHeight = 0f
    private var connectedViewBottomHalfHeight = 0f

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SceytToReplyLineView)
        isToLeft = a.getBoolean(R.styleable.SceytToReplyLineView_sceytToReplyLineViewToLeft, false)
        strokeColor = a.getColor(R.styleable.SceytToReplyLineView_sceytToReplyLineViewStrokeColor, strokeColor)
        connectedViewTopId = a.getResourceId(R.styleable.SceytToReplyLineView_sceytToReplyLineViewConnectedViewTop, 0)
        connectedViewBottomId = a.getResourceId(R.styleable.SceytToReplyLineView_sceytToReplyLineViewConnectedViewBottom, 0)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        measureConnectedViews()

        if (isToLeft)
            drawToLeft(canvas)
        else drawToRight(canvas)
    }

    // This is working fine, when connected view size not changing
    private fun measureConnectedViews() {
        if (connectedViewTopId != 0 && connectedViewTopHalfHeight == 0f) {
            val viewTop = rootView.findViewById<View>(connectedViewTopId)
            viewTop.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            connectedViewTopHalfHeight = (viewTop.measuredHeight / 2).toFloat()
        }

        if (connectedViewBottomId != 0 && connectedViewBottomHalfHeight == 0f) {
            val viewBottom = rootView.findViewById<TextView>(connectedViewBottomId)
            viewBottom.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            connectedViewBottomHalfHeight = (viewBottom.measuredHeight / 2 + paddingTop + paddingBottom).toFloat()
        }
    }

    private fun initPaint() {
        paint.color = strokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
    }

    private fun drawToRight(canvas: Canvas) {
        path.reset()
        initPaint()

        path.moveTo(strokeWidth / 2, connectedViewTopHalfHeight)
        path.lineTo(strokeWidth / 2, height - width - connectedViewBottomHalfHeight)

        if (width < height) {
            path.lineTo(strokeWidth / 2, (height - width - connectedViewBottomHalfHeight))
            path.cubicTo(strokeWidth / 2, (height - width - connectedViewBottomHalfHeight),
                strokeWidth / 2, height - connectedViewBottomHalfHeight,
                width.toFloat(), height - strokeWidth / 2 - connectedViewBottomHalfHeight)
        } else {
            path.lineTo(strokeWidth / 2, connectedViewBottomHalfHeight)
            path.cubicTo(strokeWidth / 2, connectedViewBottomHalfHeight, strokeWidth / 2, height - connectedViewBottomHalfHeight,
                width.toFloat(), height - strokeWidth / 2 - connectedViewBottomHalfHeight)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawToLeft(canvas: Canvas) {
        path.reset()
        initPaint()

        path.moveTo(width - strokeWidth / 2, connectedViewTopHalfHeight)

        if (width < height) {
            path.lineTo(width - strokeWidth / 2, (height - width - connectedViewBottomHalfHeight))
            path.cubicTo(width - strokeWidth / 2, (height - width - connectedViewBottomHalfHeight), width.toFloat(), height - connectedViewBottomHalfHeight,
                0f, height - strokeWidth / 2 - connectedViewBottomHalfHeight)
        } else {
            path.lineTo(width - strokeWidth / 2, connectedViewBottomHalfHeight)
            path.cubicTo(width - strokeWidth / 2, connectedViewBottomHalfHeight, width.toFloat(), height - connectedViewBottomHalfHeight,
                0f, height - strokeWidth / 2 - connectedViewBottomHalfHeight)
        }
        canvas.drawPath(path, paint)
    }
}