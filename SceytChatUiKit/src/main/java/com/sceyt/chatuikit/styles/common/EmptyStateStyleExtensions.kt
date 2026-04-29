package com.sceyt.chatuikit.styles.common

import android.content.Context
import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor

fun buildEmptyStateStyle(
    context: Context,
    @DrawableRes iconRes: Int,
    titleText: String? = null,
    subtitleText: String? = null
): EmptyStateStyle {
    return EmptyStateStyle.Builder(context).apply {
        setIcon(iconRes)
        setIconTint(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
        setTitleText(titleText)
        setTitleStyle(
            TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                font = R.font.roboto_medium_font
            )
        )
        subtitleText?.let {
            setSubtitleText(it)
            setSubtitleStyle(
                TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
                )
            )
        }
    }.build()
}