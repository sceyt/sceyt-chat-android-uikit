package com.sceyt.chatuikit.presentation.common

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan

class RoundedBackgroundSpan(
        private val backgroundColor: Int,
        private val cornerRadius: Float,
        private val spanStart: Int,
        private val spanEnd: Int
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
        // Find the intersection of the current line with our span
        val lineStart = start
        val lineEnd = end
        val intersectionStart = maxOf(lineStart, spanStart)
        val intersectionEnd = minOf(lineEnd, spanEnd)
        
        // Only draw if there's an intersection
        if (intersectionStart < intersectionEnd) {
            // Get the text portion that's on this line and before our span
            val textBeforeSpan = text.subSequence(lineStart, intersectionStart)
            val spanTextOnLine = text.subSequence(intersectionStart, intersectionEnd)
            
            // Measure the text widths using the actual paint object
            val textBeforeWidth = paint.measureText(textBeforeSpan, 0, textBeforeSpan.length)
            val spanWidth = paint.measureText(spanTextOnLine, 0, spanTextOnLine.length)
            
            // Calculate the actual span bounds on this line
            val spanLeft = left + textBeforeWidth
            val spanRight = spanLeft + spanWidth
            
            // Add some padding for better visual appearance
            val padding = 4f
            val rect = RectF(
                spanLeft - padding, 
                top.toFloat(), 
                spanRight + padding, 
                bottom.toFloat()
            )
            
            // Draw rounded rectangle background only for the span area
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, this.paint)
        }
    }
}