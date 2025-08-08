package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoTabBarStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val indicatorColor: Int,
        @param:ColorInt val bottomBorderColor: Int,
        @param:ColorInt val textColor: Int,
        @param:ColorInt val selectedTextColor: Int,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoTabBarStyle> { _, style -> style }
    }

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
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
