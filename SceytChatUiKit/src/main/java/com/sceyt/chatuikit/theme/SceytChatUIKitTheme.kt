package com.sceyt.chatuikit.theme

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R

data class SceytChatUIKitTheme(
        @ColorRes
        val primaryColor: Int = R.color.sceyt_color_primary,
        @ColorRes
        val statusBarColor: Int = R.color.sceyt_color_status_bar,
        @ColorRes
        val accentColor: Int = R.color.sceyt_color_accent,
        @ColorRes
        val accentColor2: Int = R.color.sceyt_color_accent_2,
        @ColorRes
        val accentColor3: Int = R.color.sceyt_color_accent_3,
        @ColorRes
        val accentColor4: Int = R.color.sceyt_color_accent_4,
        @ColorRes
        val accentColor5: Int = R.color.sceyt_color_accent_5,
        @ColorRes
        val onPrimaryColor: Int = R.color.sceyt_color_on_primary,
        @ColorRes
        val backgroundColor: Int = R.color.sceyt_color_background,
        @ColorRes
        val backgroundColorSecondary: Int = R.color.sceyt_color_background_secondary,
        @ColorRes
        val backgroundColorSections: Int = R.color.sceyt_color_background_sections,
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
        val borderColor: Int = R.color.sceyt_color_border,
        @ColorRes
        val iconSecondaryColor: Int = R.color.sceyt_color_icon_secondary,
        @ColorRes
        val iconInactiveColor: Int = R.color.sceyt_color_icon_inactive,
        @ColorRes
        val textPrimaryColor: Int = R.color.sceyt_color_text_primary,
        @ColorRes
        val textSecondaryColor: Int = R.color.sceyt_color_text_secondary,
        @ColorRes
        val textFootnoteColor: Int = R.color.sceyt_color_text_footnote,
        @ColorRes
        val errorColor: Int = R.color.sceyt_color_error,
        @ColorRes
        val successColor: Int = R.color.sceyt_color_green,
        @ColorRes
        val warningColor: Int = R.color.sceyt_color_warning,
        @DrawableRes
        val userDefaultAvatar: Int = R.drawable.sceyt_ic_default_avatar,
        @DrawableRes
        val deletedUserAvatar: Int = R.drawable.sceyt_ic_deleted_user,
        @DrawableRes
        val notesAvatar: Int = R.drawable.sceyt_ic_notes_with_paddings,
)