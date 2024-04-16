package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.pxToDp
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ChannelDateFormatter

data class ChannelListViewStyle(
        val context: Context,

        @ColorInt
        val backgroundColor: Int = context.getCompatColor(R.color.sceyt_color_bg),

        @ColorInt
        val pinnedChannelBackgroundColor: Int = context.getCompatColor(R.color.sceyt_color_gray_themed),

        @ColorInt
        val titleColor: Int = context.getCompatColor(R.color.sceyt_color_text_themed),

        @ColorInt
        val lastMessageTextColor: Int = context.getCompatColor(R.color.sceyt_color_gray_400),

        @ColorRes
        val unreadCountColor: Int = SceytKitConfig.sceytColorAccent,

        @ColorInt
        val onlineStatusColor: Int = context.getCompatColor(R.color.sceyt_color_green),

        @ColorInt
        val dividerColor: Int = context.getCompatColor(R.color.sceyt_color_divider),

        @ColorInt
        val dateTextColor: Int = context.getCompatColor(R.color.sceyt_color_gray_400),

        val mutedIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_muted),

        val statusIndicatorPendingIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_not_sent),

        val statusIndicatorSentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_on_server),

        val statusIndicatorDeliveredIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered),

        val statusIndicatorReadIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_read),

        val bodyFileAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_file_attachment),

        val bodyImageAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_image_attachment),

        val bodyVideoAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_video_attachment),

        val bodyVoiceAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_body_voice_attachment),

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

        val enableDivider: Boolean = true,

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
                backgroundColor = typedArray.getColor(R.styleable.ChannelsListView_sceytUiChannelListBackgroundColor, backgroundColor),
                pinnedChannelBackgroundColor = typedArray.getColor(R.styleable.ChannelsListView_sceytUiChannelListPinnedBackgroundColor, pinnedChannelBackgroundColor),
                titleColor = typedArray.getColor(R.styleable.ChannelsListView_sceytUiChannelTitleTextColor, titleColor),
                lastMessageTextColor = typedArray.getColor(R.styleable.ChannelsListView_sceytUiLastMessageTextColor, lastMessageTextColor),
                unreadCountColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiUnreadMessageCounterTextColor, unreadCountColor),
                mutedIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiMutedChannelIcon)
                        ?: mutedIcon,
                dateTextColor = typedArray.getColor(R.styleable.ChannelsListView_sceytUiLastMessageDateTextColor, dateTextColor),
                statusIndicatorPendingIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiIndicatorPendingIcon)
                        ?: statusIndicatorPendingIcon,
                statusIndicatorSentIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiIndicatorSentIcon)
                        ?: statusIndicatorSentIcon,
                statusIndicatorDeliveredIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiIndicatorDeliveredIcon)
                        ?: statusIndicatorDeliveredIcon,
                statusIndicatorReadIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiIndicatorReadIcon)
                        ?: statusIndicatorReadIcon,
                bodyFileAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiBodyFileAttachmentIcon)
                        ?: bodyFileAttachmentIcon,
                bodyImageAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiBodyImageAttachmentIcon)
                        ?: bodyImageAttachmentIcon,
                bodyVideoAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiBodyVideoAttachmentIcon)
                        ?: bodyVideoAttachmentIcon,
                bodyVoiceAttachmentIcon = typedArray.getDrawable(R.styleable.ChannelsListView_sceytUiBodyVoiceAttachmentIcon)
                        ?: bodyVoiceAttachmentIcon,
                statusIconSize = typedArray.getDimensionPixelSize(R.styleable.ChannelsListView_sceytUiStatusIndicatorSize, statusIconSize),
                emptyState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptyStateView, emptyState),
                emptySearchState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptySearchStateView, emptySearchState),
                loadingState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLoadingView, loadingState),
                onlineStatusColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiOnlineIndicatorColor, onlineStatusColor),
                dividerColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiDividerColor, dividerColor),
                enableDivider = typedArray.getBoolean(R.styleable.ChannelsListView_sceytUiEnableDivider, enableDivider),
                showChannelActionAsPopup = typedArray.getBoolean(R.styleable.ChannelsListView_sceytUiShowChannelActionAsPopup, showChannelActionAsPopup),
                popupStyle = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiPopupStyle, popupStyle),
            )
        }.let(channelStyleCustomizer::apply)
    }
}