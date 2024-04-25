package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.pxToDp
import com.sceyt.chatuikit.presentation.uicomponents.channels.ChannelListView
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ChannelDateFormatter
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [ChannelListView] component.
 * @property backgroundColor - Background color of the channel list, default is [SceytChatUIKitTheme.backgroundColor].
 * @property pinnedChannelBackgroundColor - Background color of the pinned channel, default is [SceytChatUIKitTheme.surface1Color].
 * @property titleColor - Color of the channel title, default is [SceytChatUIKitTheme.textPrimaryColor].
 * @property lastMessageTextColor - Color of the last message text, default is [SceytChatUIKitTheme.textSecondaryColor].
 * @property unreadCountColor - Color of the unread message counter, default is [SceytChatUIKitTheme.accentColor].
 * @property onlineStatusColor - Color of the online status indicator, default is [R.color.sceyt_color_green].
 * @property dividerColor - Color of the divider, default is [SceytChatUIKitTheme.bordersColor].
 * @property dateTextColor - Color of the date text, default is [SceytChatUIKitTheme.textSecondaryColor].
 * @property mutedIcon - Icon for muted channel, default is [R.drawable.sceyt_ic_muted].
 * @property pinIcon - Icon for pinned channel, default is [R.drawable.sceyt_ic_pin_filled].
 * @property statusIndicatorPendingIcon - Icon for pending status, default is [R.drawable.sceyt_ic_status_not_sent].
 * @property statusIndicatorSentIcon - Icon for sent status, default is [R.drawable.sceyt_ic_status_on_server].
 * @property statusIndicatorDeliveredIcon - Icon for delivered status, default is [R.drawable.sceyt_ic_status_delivered].
 * @property statusIndicatorReadIcon - Icon for read status, default is [R.drawable.sceyt_ic_status_read].
 * @property bodyFileAttachmentIcon - Icon for file attachment, default is [R.drawable.sceyt_ic_body_file_attachment].
 * @property bodyImageAttachmentIcon - Icon for image attachment, default is [R.drawable.sceyt_ic_body_image_attachment].
 * @property bodyVideoAttachmentIcon - Icon for video attachment, default is [R.drawable.sceyt_ic_body_video_attachment].
 * @property bodyVoiceAttachmentIcon - Icon for voice attachment, default is [R.drawable.sceyt_ic_body_voice_attachment].
 * @property emptyState - Layout for empty state, default is [R.layout.sceyt_channel_list_empty_state].
 * @property emptySearchState - Layout for empty search state, default is [R.layout.sceyt_search_channels_empty_state].
 * @property loadingState - Layout for loading state, default is [R.layout.sceyt_channels_page_loading_state].
 * @property popupStyle - Style for popup, default is [R.style.SceytPopupMenuStyle].
 * @property channelDateFormat - Date format for channel, default is [ChannelDateFormatter].
 * @property showChannelActionAsPopup - Show channel action as popup, default is false.
 * @property enableDivider - Enable divider, default is false.
 * @property statusIconSize - Size of the status icon, default is 16dp.
 * */
data class ChannelListViewStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val pinnedChannelBackgroundColor: Int,
        @ColorInt val titleColor: Int,
        @ColorInt val lastMessageTextColor: Int,
        @ColorInt val unreadCountColor: Int,
        @ColorInt val onlineStatusColor: Int,
        @ColorInt val dividerColor: Int,
        @ColorInt val dateTextColor: Int,
        val mutedIcon: Drawable?,
        val pinIcon: Drawable?,
        val statusIndicatorPendingIcon: Drawable?,
        val statusIndicatorSentIcon: Drawable?,
        val statusIndicatorDeliveredIcon: Drawable?,
        val statusIndicatorReadIcon: Drawable?,
        val bodyFileAttachmentIcon: Drawable?,
        val bodyImageAttachmentIcon: Drawable?,
        val bodyVideoAttachmentIcon: Drawable?,
        val bodyVoiceAttachmentIcon: Drawable?,
        @LayoutRes val emptyState: Int,
        @LayoutRes val emptySearchState: Int,
        @LayoutRes val loadingState: Int,
        @StyleRes val popupStyle: Int,
        val channelDateFormat: ChannelDateFormatter,
        val showChannelActionAsPopup: Boolean,
        val enableDivider: Boolean,
        val statusIconSize: Int
) {

    companion object {
        @JvmField
        var channelStyleCustomizer = StyleCustomizer<ChannelListViewStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {

        fun build(): ChannelListViewStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChannelListView, 0, 0)

            val backgroundColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelListBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

            val pinnedChannelBackgroundColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelListPinnedBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.surface1Color))

            val titleColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelTitleTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val lastMessageTextColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiLastMessageTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val unreadCountColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val mutedIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiMutedChannelIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_muted)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val pinIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiPinnedChannelIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_pin_filled)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val dateTextColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiLastMessageDateTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val statusIndicatorPendingIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorPendingIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_not_sent)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val statusIndicatorSentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorSentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_on_server)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val statusIndicatorDeliveredIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorDeliveredIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val statusIndicatorReadIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorReadIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_read)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
                    }

            val bodyFileAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyFileAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_body_file_attachment)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val bodyImageAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyImageAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_body_image_attachment)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val bodyVideoAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyVideoAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_body_video_attachment)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val bodyVoiceAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyVoiceAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_body_voice_attachment)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val statusIconSize = typedArray.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiStatusIndicatorSize,
                pxToDp(16f).toInt())

            val emptyState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiEmptyStateView,
                R.layout.sceyt_channel_list_empty_state)

            val emptySearchState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiEmptySearchStateView,
                R.layout.sceyt_search_channels_empty_state)

            val loadingState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiLoadingView,
                R.layout.sceyt_channels_page_loading_state)

            val onlineStatusColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiOnlineIndicatorColor,
                context.getCompatColor(R.color.sceyt_color_green))

            val dividerColor = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiDividerColor,
                context.getCompatColor(SceytChatUIKit.theme.bordersColor))

            val enableDivider = typedArray.getBoolean(R.styleable.ChannelListView_sceytUiEnableDivider, false)

            val showChannelActionAsPopup = typedArray.getBoolean(R.styleable.ChannelListView_sceytUiShowChannelActionAsPopup, false)

            val popupStyle = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiPopupStyle,
                R.style.SceytPopupMenuStyle)

            typedArray.recycle()

            return ChannelListViewStyle(
                backgroundColor = backgroundColor,
                pinnedChannelBackgroundColor = pinnedChannelBackgroundColor,
                titleColor = titleColor,
                lastMessageTextColor = lastMessageTextColor,
                unreadCountColor = unreadCountColor,
                onlineStatusColor = onlineStatusColor,
                dividerColor = dividerColor,
                dateTextColor = dateTextColor,
                mutedIcon = mutedIcon,
                pinIcon = pinIcon,
                statusIndicatorPendingIcon = statusIndicatorPendingIcon,
                statusIndicatorSentIcon = statusIndicatorSentIcon,
                statusIndicatorDeliveredIcon = statusIndicatorDeliveredIcon,
                statusIndicatorReadIcon = statusIndicatorReadIcon,
                bodyFileAttachmentIcon = bodyFileAttachmentIcon,
                bodyImageAttachmentIcon = bodyImageAttachmentIcon,
                bodyVideoAttachmentIcon = bodyVideoAttachmentIcon,
                bodyVoiceAttachmentIcon = bodyVoiceAttachmentIcon,
                emptyState = emptyState,
                emptySearchState = emptySearchState,
                loadingState = loadingState,
                popupStyle = popupStyle,
                statusIconSize = statusIconSize,
                channelDateFormat = ChannelDateFormatter(),
                showChannelActionAsPopup = showChannelActionAsPopup,
                enableDivider = enableDivider
            ).let(channelStyleCustomizer::apply)
        }
    }
}