package com.sceyt.chatuikit.presentation.common

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

class RoundedBackgroundSpan(
        private val backgroundColor: Int,
        private val cornerRadius: Float,
) : ReplacementSpan() {

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = backgroundColor
        style = Paint.Style.FILL
    }

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        text ?: return

        val textWidth = paint.measureText(text, start, end)

        val rect = RectF(x, top.toFloat(), x + textWidth, bottom.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}