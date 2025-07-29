package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [ChannelInfoActivity] page.
 * @property backgroundColor - background color of the page, default value is [Colors.backgroundColor]
 * @property borderColor - border color of the borders, default value is [Colors.borderColor]
 * @property dividerColor - border color of the dividers, default value is [Colors.backgroundColorSecondary]
 * @property spaceBetweenSections - space between sections, default value is 16dp
 * @property toolBarStyle - style for the toolbar
 * @property detailsStyle - style for the details section
 * @property descriptionStyle - style for the description section
 * @property uriStyle - style for the uri section
 * @property settingsStyle - style for the settings section
 * @property optionsStyle - style for the options section
 * @property tabBarStyle - style for the tab bar
 * */
data class ChannelInfoStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val borderColor: Int,
        @ColorInt val dividerColor: Int,
        @Px var spaceBetweenSections: Int,
        val toolBarStyle: ChannelInfoToolBarStyle,
        val detailsStyle: ChannelInfoDetailStyle,
        val descriptionStyle: ChannelInfoDescriptionStyle,
        val uriStyle: ChannelInfoURIStyle,
        val settingsStyle: ChannelInfoSettingsStyle,
        val optionsStyle: ChannelInfoOptionsStyle,
        val tabBarStyle: ChannelInfoTabBarStyle,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {

        fun build(): ChannelInfoStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)
            val borderColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor)
            val dividerColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColorSecondary)
            val spaceBetweenSections = dpToPx(16f)
            val toolBarStyle = ChannelInfoToolBarStyle.Builder(context, attributeSet).build()
            val detailsStyle = ChannelInfoDetailStyle.Builder(context, attributeSet).build()
            val descriptionStyle = ChannelInfoDescriptionStyle.Builder(context, attributeSet).build()
            val specificationsStyle = ChannelInfoURIStyle.Builder(context, attributeSet).build()
            val settingsStyle = ChannelInfoSettingsStyle.Builder(context, attributeSet).build()
            val optionsStyle = ChannelInfoOptionsStyle.Builder(context, attributeSet).build()
            val tabBarStyle = ChannelInfoTabBarStyle.Builder(context, attributeSet).build()

            return ChannelInfoStyle(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                dividerColor = dividerColor,
                spaceBetweenSections = spaceBetweenSections,
                toolBarStyle = toolBarStyle,
                detailsStyle = detailsStyle,
                descriptionStyle = descriptionStyle,
                uriStyle = specificationsStyle,
                settingsStyle = settingsStyle,
                optionsStyle = optionsStyle,
                tabBarStyle = tabBarStyle,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
