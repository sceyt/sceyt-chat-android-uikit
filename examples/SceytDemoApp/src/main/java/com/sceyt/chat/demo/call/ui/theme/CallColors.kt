package com.sceyt.chat.demo.call.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for call screens.
 */
object CallColors {
    // Primary screen background (Figma: #19191B)
    val BackgroundDark = Color(0xFF19191B)

    // Gradient backgrounds (used for video call layout)
    val GradientStart = Color(0xFF1A1A2E)
    val GradientMiddle = Color(0xFF16213E)
    val GradientEnd = Color(0xFF0F0F1A)

    // Accent colors
    val AccentBlue = Color(0xFF4F8CFF)
    val AccentGreen = Color(0xFF34C759)
    val AccentRed = Color(0xFFFF453A)
    val AccentOrange = Color(0xFFFF9F0A)
    val AccentYellow = Color(0xFFFFD60A)

    // New Figma design colors
    val HangupRed = Color(0xFFFA4C56)          // Hangup button
    val CallAgainGreen = Color(0xFF23BE5D)     // Call Again button
    val ButtonSurface = Color(0xB33B3B3D)     // rgba(59,59,61,0.7) - inactive buttons
    val ActionBarBg = Color(0x99303032)        // rgba(48,48,50,0.6) - action bar container
    val DurationPillBg = Color(0x993B3B3D)    // rgba(59,59,61,0.6) - duration pill

    // Surface colors
    val SurfaceLight = Color(0xFF2A2A40)
    val SurfaceDark = Color(0xFF1A1A28)
    val SurfaceOverlay = Color(0xFF252535)

    // Text colors
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB0B0C0)
    val TextTertiary = Color(0xFF707080)
}
