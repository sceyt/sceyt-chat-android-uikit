package com.sceyt.chatuikit.presentation.common

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan

class RoundedBackgroundSpan(
        private val backgroundColor: Int,
        private val cornerRadius: Float,
) : LineBackgroundSpan {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = backgroundColor
    }

    override fun drawBackground(
            canvas: Canvas, paint: Paint,
            left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int, lnum: Int
    ) {
        val rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        // Draw rounded rectangle background only
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, this.paint)
    }
}