package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoOptionsStyle(
        @ColorInt val backgroundColor: Int,
        val titleTextStyle: TextStyle,
        val membersIcon: Drawable?,
        val adminsIcon: Drawable?,
        val subscribersIcon: Drawable?,
        val searchIcon: Drawable?,
        val subscribersTitleText: String,
        val membersTitleText: String,
        val adminsTitleText: String,
        val searchMessagesTitleText: String
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoOptionsStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoOptionsStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
            )

            val membersIcon = context.getCompatDrawable(R.drawable.sceyt_ic_members_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor4, R.id.backgroundLayer)

            val adminsIcon = context.getCompatDrawable(R.drawable.sceyt_ic_admin_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor5, R.id.backgroundLayer)

            val searchIcon = context.getCompatDrawable(R.drawable.sceyt_ic_search_messages_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor, R.id.backgroundLayer)

            val subscribersTitleText = context.getString(R.string.sceyt_subscribers)

            val membersTitleText = context.getString(R.string.sceyt_members)

            val adminsTitleText = context.getString(R.string.sceyt_admins)

            val searchMessagesTitleText = context.getString(R.string.sceyt_search_messages)

            return ChannelInfoOptionsStyle(
                backgroundColor = backgroundColor,
                titleTextStyle = titleTextStyle,
                membersIcon = membersIcon,
                subscribersIcon = membersIcon,
                adminsIcon = adminsIcon,
                searchIcon = searchIcon,
                subscribersTitleText = subscribersTitleText,
                membersTitleText = membersTitleText,
                adminsTitleText = adminsTitleText,
                searchMessagesTitleText = searchMessagesTitleText
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}