package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.pxToDp
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ChannelDateFormatter

data class ChannelListViewStyle(
        val context: Context,

        @ColorInt
        val backgroundColor: Int = context.getCompatColor(SceytChatUIKit.theme.backgroundColor),

        @ColorInt
        val pinnedChannelBackgroundColor: Int = context.getCompatColor(SceytChatUIKit.theme.surface1Color),

        @ColorInt
        val titleColor: Int = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor),

        @ColorInt
        val lastMessageTextColor: Int = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor),

        @ColorInt
        val unreadCountColor: Int = context.getCompatColor(SceytChatUIKit.theme.accentColor),

        @ColorInt
        val onlineStatusColor: Int = context.getCompatColor(R.color.sceyt_color_green),

        @ColorInt
        val dividerColor: Int = context.getCompatColor(SceytChatUIKit.theme.bordersColor),

        @ColorInt
        val dateTextColor: Int = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor),

        val mutedIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_muted)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val pinIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_pin_filled)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val statusIndicatorPendingIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_not_sent)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val statusIndicatorSentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_on_server)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val statusIndicatorDeliveredIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val statusIndicatorReadIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_read).also {
            it?.setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        },

        val bodyFileAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_file_attachment)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val bodyImageAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_image_attachment)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val bodyVideoAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_video_attachment)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        val bodyVoiceAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_voice_attachment)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
        },

        @LayoutRes
        val emptyState: Int = R.layout.sceyt_channel_list_empty_state,

        @LayoutRes
        val emptySearchState: Int = R.layout.sceyt_search_channels_empty_state,

        @LayoutRes
        val loadingState: Int = R.layout.sceyt_channels_page_loading_state,

        @StyleRes
        val popupStyle: Int = R.style.SceytPopupMenuStyle,

        val channelDateFormat: ChannelDateFormatter = ChannelDateFormatter(),

        val showChannelActionAsPopup: Boolean = false,

        val enableDivider: Boolean = false,

        val statusIconSize: Int = pxToDp(16f).toInt()
) {

    companion object {
        @JvmField
        var channelStyleCustomizer = StyleCustomizer<ChannelListViewStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {

        fun build() = ChannelListViewStyle(context).run {
            copy(
                backgroundColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelListBackgroundColor, backgroundColor),
                pinnedChannelBackgroundColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelListPinnedBackgroundColor, pinnedChannelBackgroundColor),
                titleColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelTitleTextColor, titleColor),
                lastMessageTextColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiLastMessageTextColor, lastMessageTextColor),
                unreadCountColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor, unreadCountColor),
                mutedIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiMutedChannelIcon)
                        ?: mutedIcon,
                pinIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiPinnedChannelIcon)
                        ?: pinIcon,
                dateTextColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiLastMessageDateTextColor, dateTextColor),
                statusIndicatorPendingIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorPendingIcon)
                        ?: statusIndicatorPendingIcon,
                statusIndicatorSentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorSentIcon)
                        ?: statusIndicatorSentIcon,
                statusIndicatorDeliveredIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorDeliveredIcon)
                        ?: statusIndicatorDeliveredIcon,
                statusIndicatorReadIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiIndicatorReadIcon)
                        ?: statusIndicatorReadIcon,
                bodyFileAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyFileAttachmentIcon)
                        ?: bodyFileAttachmentIcon,
                bodyImageAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyImageAttachmentIcon)
                        ?: bodyImageAttachmentIcon,
                bodyVideoAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyVideoAttachmentIcon)
                        ?: bodyVideoAttachmentIcon,
                bodyVoiceAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelListView_sceytUiBodyVoiceAttachmentIcon)
                        ?: bodyVoiceAttachmentIcon,
                statusIconSize = typedArray.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiStatusIndicatorSize, statusIconSize),
                emptyState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiEmptyStateView, emptyState),
                emptySearchState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiEmptySearchStateView, emptySearchState),
                loadingState = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiLoadingView, loadingState),
                onlineStatusColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiOnlineIndicatorColor, onlineStatusColor),
                dividerColor = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiDividerColor, dividerColor),
                enableDivider = typedArray.getBoolean(R.styleable.ChannelListView_sceytUiEnableDivider, enableDivider),
                showChannelActionAsPopup = typedArray.getBoolean(R.styleable.ChannelListView_sceytUiShowChannelActionAsPopup, showChannelActionAsPopup),
                popupStyle = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiPopupStyle, popupStyle),
            )
        }.let(channelStyleCustomizer::apply)
    }
}