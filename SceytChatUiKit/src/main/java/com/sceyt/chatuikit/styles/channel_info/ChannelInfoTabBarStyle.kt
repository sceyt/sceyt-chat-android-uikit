package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoTabBarStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val indicatorColor: Int,
        @ColorInt val bottomBorderColor: Int,
        @ColorInt val textColor: Int,
        @ColorInt val selectedTextColor: Int,
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoTabBarStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
            val indicatorColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            val bottomBorderColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)

            val textColor = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor)

            val selectedTextColor = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)

            return ChannelInfoTabBarStyle(
                backgroundColor = backgroundColor,
                indicatorColor = indicatorColor,
                bottomBorderColor = bottomBorderColor,
                textColor = textColor,
                selectedTextColor = selectedTextColor
            )
        }
    }
}
