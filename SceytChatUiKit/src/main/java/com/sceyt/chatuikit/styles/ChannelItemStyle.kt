package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.pxToDp
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.Provider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelListAttachmentIconProvider
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/**
 * Style for [ChannelItemStyle] component.
 * @property pinnedChannelBackgroundColor - Background color of the pinned channel, default is [SceytChatUIKitTheme.surface1Color].
 * @property titleColor - Color of the channel title, default is [SceytChatUIKitTheme.textPrimaryColor].
 * @property lastMessageTextColor - Color of the last message text, default is [SceytChatUIKitTheme.textSecondaryColor].
 * @property unreadCountColor - Color of the unread message counter, default is [SceytChatUIKitTheme.accentColor].
 * @property onlineStatusColor - Color of the online status indicator, default is [R.color.sceyt_color_green].
 * @property dividerColor - Color of the divider, default is [SceytChatUIKitTheme.borderColor].
 * @property dateTextColor - Color of the date text, default is [SceytChatUIKitTheme.textSecondaryColor].
 * @property enableDivider - Enable divider, default is false.
 * @property mutedIcon - Icon for muted channel, default is [R.drawable.sceyt_ic_muted].
 * @property pinIcon - Icon for pinned channel, default is [R.drawable.sceyt_ic_pin_filled].
 * @property statusIconSize - Size of the status icon, default is 16dp.
 * @property channelDateFormat - Date format for channel, default is [DefaultChannelDateFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [DefaultChannelListAttachmentIconProvider].
 * */
data class ChannelItemStyle(
        @ColorInt val pinnedChannelBackgroundColor: Int,
        @ColorInt val titleColor: Int,
        @ColorInt val lastMessageTextColor: Int,
        @ColorInt val unreadCountColor: Int,
        @ColorInt val onlineStatusColor: Int,
        @ColorInt val dividerColor: Int,
        @ColorInt val dateTextColor: Int,
        val enableDivider: Boolean,
        val mutedIcon: Drawable?,
        val pinIcon: Drawable?,
        val messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
        val autoDeletedChannelIcon: Drawable?,
        val statusIconSize: Int,
        val channelDateFormat: Formatter<Date>,
        val attachmentIconProvider: Provider<SceytAttachment, Drawable?>
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<ChannelItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): ChannelItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ChannelListView).use { array ->
                val pinnedChannelBackgroundColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListPinnedBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.surface1Color))

                val titleColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelTitleTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

                val lastMessageTextColor = array.getColor(R.styleable.ChannelListView_sceytUiLastMessageTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

                val unreadCountColor = array.getColor(R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.accentColor))

                val onlineStatusColor = array.getColor(R.styleable.ChannelListView_sceytUiOnlineIndicatorColor,
                    context.getCompatColor(R.color.sceyt_color_green))

                val dividerColor = array.getColor(R.styleable.ChannelListView_sceytUiDividerColor,
                    context.getCompatColor(SceytChatUIKit.theme.borderColor))

                val mutedIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiMutedChannelIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_muted)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val pinIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiPinnedChannelIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pin_filled)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val enableDivider = array.getBoolean(R.styleable.ChannelListView_sceytUiEnableDivider, false)

                val dateTextColor = array.getColor(R.styleable.ChannelListView_sceytUiLastMessageDateTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

                val messageDeliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.ChannelListView_sceytUiIndicatorPendingIcon)
                    .setSentIconFromStyle(R.styleable.ChannelListView_sceytUiIndicatorSentIcon)
                    .setReceivedIconIconFromStyle(R.styleable.ChannelListView_sceytUiIndicatorReceivedIcon)
                    .setDisplayedIconFromStyle(R.styleable.ChannelListView_sceytUiIndicatorDisplayedIcon)
                    .build()

                val autoDeletedChannelIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiAutoDeletedChannelIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_auto_deleted_channel)

                val statusIconSize = array.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiStatusIndicatorSize,
                    pxToDp(16f).toInt())

                return ChannelItemStyle(
                    pinnedChannelBackgroundColor = pinnedChannelBackgroundColor,
                    titleColor = titleColor,
                    lastMessageTextColor = lastMessageTextColor,
                    unreadCountColor = unreadCountColor,
                    onlineStatusColor = onlineStatusColor,
                    dividerColor = dividerColor,
                    dateTextColor = dateTextColor,
                    enableDivider = enableDivider,
                    mutedIcon = mutedIcon,
                    pinIcon = pinIcon,
                    messageDeliveryStatusIcons = messageDeliveryStatusIcons,
                    autoDeletedChannelIcon = autoDeletedChannelIcon,
                    statusIconSize = statusIconSize,
                    channelDateFormat = SceytChatUIKit.formatters.channelDateFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}