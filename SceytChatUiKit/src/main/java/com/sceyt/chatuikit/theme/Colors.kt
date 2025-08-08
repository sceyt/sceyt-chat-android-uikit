package com.sceyt.chatuikit.theme

import androidx.annotation.ColorRes
import com.sceyt.chatuikit.R

data class Colors(
        @param:ColorRes
        val primaryColor: Int = R.color.sceyt_color_primary,
        @param:ColorRes
        val statusBarColor: Int = R.color.sceyt_color_status_bar,
        @param:ColorRes
        val accentColor: Int = R.color.sceyt_color_accent,
        @param:ColorRes
        val accentColor2: Int = R.color.sceyt_color_accent_2,
        @param:ColorRes
        val accentColor3: Int = R.color.sceyt_color_accent_3,
        @param:ColorRes
        val accentColor4: Int = R.color.sceyt_color_accent_4,
        @param:ColorRes
        val accentColor5: Int = R.color.sceyt_color_accent_5,
        @param:ColorRes
        val onPrimaryColor: Int = R.color.sceyt_color_on_primary,
        @param:ColorRes
        val backgroundColor: Int = R.color.sceyt_color_background,
        @param:ColorRes
        val backgroundColorSecondary: Int = R.color.sceyt_color_background_secondary,
        @param:ColorRes
        val backgroundColorSections: Int = R.color.sceyt_color_background_sections,
        @param:ColorRes
        val surface1Color: Int = R.color.sceyt_color_surface_1,
        @param:ColorRes
        val surface2Color: Int = R.color.sceyt_color_surface_2,
        @param:ColorRes
        val surface3Color: Int = R.color.sceyt_color_surface_3,
        @param:ColorRes
        val overlayBackgroundColor: Int = R.color.sceyt_color_overlay_background,
        @param:ColorRes
        val overlayBackground2Color: Int = R.color.sceyt_color_overlay_background_2,
        @param:ColorRes
        val borderColor: Int = R.color.sceyt_color_border,
        @param:ColorRes
        val iconSecondaryColor: Int = R.color.sceyt_color_icon_secondary,
        @param:ColorRes
        val iconInactiveColor: Int = R.color.sceyt_color_icon_inactive,
        @param:ColorRes
        val textPrimaryColor: Int = R.color.sceyt_color_text_primary,
        @param:ColorRes
        val textSecondaryColor: Int = R.color.sceyt_color_text_secondary,
        @param:ColorRes
        val textFootnoteColor: Int = R.color.sceyt_color_text_footnote,
        @param:ColorRes
        val warningColor: Int = R.color.sceyt_color_warning,
        @param:ColorRes
        val successColor: Int = R.color.sceyt_color_green,
        @param:ColorRes
        val attentionColor: Int = R.color.sceyt_color_attention,
)
