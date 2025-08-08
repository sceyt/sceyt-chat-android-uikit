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

data class ChannelInfoSettingsStyle(
        @param:ColorInt val backgroundColor: Int,
        val notificationsIcon: Drawable?,
        val autoDeleteMessagesIcon: Drawable?,
        val notificationsTitleText: String,
        val autoDeleteMessagesTitleText: String,
        val titleTextStyle: TextStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoSettingsStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoSettingsStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val notificationsTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
            )

            val notificationsIcon = context.getCompatDrawable(R.drawable.sceyt_ic_notification_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor2, R.id.backgroundLayer)

            val autoDeleteMessagesIcon = context.getCompatDrawable(R.drawable.sceyt_ic_auto_delete_messages_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor3, R.id.backgroundLayer)

            val notificationsTitleText = context.getString(R.string.sceyt_notifications)
            val autoDeleteMessagesTitleText = context.getString(R.string.sceyt_auto_delete_messages_off)

            return ChannelInfoSettingsStyle(
                backgroundColor = backgroundColor,
                titleTextStyle = notificationsTextStyle,
                notificationsIcon = notificationsIcon,
                notificationsTitleText = notificationsTitleText,
                autoDeleteMessagesTitleText = autoDeleteMessagesTitleText,
                autoDeleteMessagesIcon = autoDeleteMessagesIcon
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
