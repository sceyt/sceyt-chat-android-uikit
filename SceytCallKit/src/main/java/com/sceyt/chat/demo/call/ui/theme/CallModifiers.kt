package com.sceyt.chat.demo.call.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Applies the Figma call screen background: a radial gradient from blue-purple at the top-left
 * corner fading to near-black toward the bottom-right.
 * Uses drawBehind so the brush has access to the actual composable size.
 */
fun Modifier.callBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.005f to Color(0xFF2E316F),
                0.393f to Color(0xFF242545),
                0.587f to Color(0xFF1E1F30),
                0.781f to Color(0xFF19191B),
                1.000f to Color(0xFF19191B),
            ),
            center = Offset.Zero,
            radius = size.height
        )
    )
}
