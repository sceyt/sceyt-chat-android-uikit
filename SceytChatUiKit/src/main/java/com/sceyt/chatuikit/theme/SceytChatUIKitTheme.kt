package com.sceyt.chatuikit.theme

import androidx.annotation.ColorRes
import com.sceyt.chatuikit.R

data class SceytChatUIKitTheme(
        @ColorRes
        val primaryColor: Int = R.color.sceyt_color_primary,
        @ColorRes
        val statusBarColor: Int = R.color.sceyt_color_status_bar,
        @ColorRes
        val accentColor: Int = R.color.sceyt_color_accent,
        @ColorRes
        val backgroundColor: Int = R.color.sceyt_color_background,
        @ColorRes
        val surface1Color: Int = R.color.sceyt_color_surface_1,
        @ColorRes
        val surface2Color: Int = R.color.sceyt_color_surface_2,
        @ColorRes
        val surface3Color: Int = R.color.sceyt_color_surface_3,
        @ColorRes
        val overlayBackgroundColor: Int = R.color.sceyt_color_overlay_background,
        @ColorRes
        val overlayBackground2Color: Int = R.color.sceyt_color_overlay_background_2,
        @ColorRes
        val bordersColor: Int = R.color.sceyt_color_border,
        @ColorRes
        val iconSecondaryColor: Int = R.color.sceyt_color_gray_secondary,
        @ColorRes
        val iconInactiveColor: Int = R.color.sceyt_color_icon_inactive,
        @ColorRes
        val textPrimaryColor: Int = R.color.sceyt_color_text_primary,
        @ColorRes
        val textSecondaryColor: Int = R.color.sceyt_color_text_secondary_themed,
        @ColorRes
        val textFootnoteColor: Int = R.color.sceyt_color_text_footnote_themed,
        @ColorRes
        val textOnPrimaryColor: Int = R.color.sceyt_color_white,
        @ColorRes
        val errorColor: Int = R.color.sceyt_color_red,
        @ColorRes
        val successColor: Int = R.color.sceyt_color_green,
        @ColorRes
        val warningColor: Int = R.color.sceyt_color_yellow,
)