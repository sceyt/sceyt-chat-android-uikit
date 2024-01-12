package com.sceyt.sceytchatuikit.sceytstyles

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.pxToDp
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.ChannelDateFormatter

object ChannelStyle {

    @JvmField
    @ColorRes
    var backgroundColor: Int = R.color.sceyt_color_bg

    @JvmField
    @ColorRes
    var titleColor: Int = R.color.sceyt_color_text_themed

    @JvmField
    @ColorRes
    var lastMessageTextColor: Int = R.color.sceyt_color_gray_400

    @JvmField
    @ColorRes
    var unreadCountColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @ColorRes
    var onlineStatusColor: Int = R.color.sceyt_color_green

    @JvmField
    @ColorRes
    var dividerColor: Int = R.color.sceyt_color_divider

    @JvmField
    @DrawableRes
    var mutedIcon: Int = R.drawable.sceyt_ic_muted

    @JvmField
    @ColorRes
    var dateTextColor: Int = R.color.sceyt_color_gray_400

    @JvmField
    @DrawableRes
    var statusIndicatorPendingIcon: Int = R.drawable.sceyt_ic_status_not_sent

    @JvmField
    @DrawableRes
    var statusIndicatorSentIcon: Int = R.drawable.sceyt_ic_status_on_server

    @JvmField
    @DrawableRes
    var statusIndicatorDeliveredIcon: Int = R.drawable.sceyt_ic_status_delivered

    @JvmField
    @DrawableRes
    var statusIndicatorReadIcon: Int = R.drawable.sceyt_ic_status_read

    @DrawableRes
    var bodyFileAttachmentIcon: Int = R.drawable.sceyt_ic_body_file_attachment

    @JvmField
    @DrawableRes
    var bodyImageAttachmentIcon: Int = R.drawable.sceyt_ic_body_image_attachment

    @JvmField
    @DrawableRes
    var bodyVideoAttachmentIcon: Int = R.drawable.sceyt_ic_body_video_attachment

    @JvmField
    @DrawableRes
    var bodyVoiceAttachmentIcon: Int = R.drawable.sceyt_ic_body_voice_attachment

    @JvmField
    var statusIconSize: Int = pxToDp(16f).toInt()

    @JvmField
    @LayoutRes
    var emptyState: Int = R.layout.sceyt_channel_list_empty_state

    @JvmField
    @LayoutRes
    var emptySearchState: Int = R.layout.sceyt_search_channels_empty_state

    @JvmField
    @LayoutRes
    var loadingState: Int = R.layout.sceyt_channels_page_loading_state

    @JvmField
    var enableDivider = true

    @JvmField
    @StyleRes
    var popupStyle: Int = R.style.SceytPopupMenuStyle

    @JvmField
    var channelDateFormat = ChannelDateFormatter()

    @JvmField
    var showChannelActionAsPopup = false

    internal fun updateWithAttributes(typedArray: TypedArray): ChannelStyle {
        backgroundColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiChannelListBackgroundColor, backgroundColor)
        titleColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiChannelTitleTextColor, titleColor)
        lastMessageTextColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLastMessageTextColor, lastMessageTextColor)
        unreadCountColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiUnreadMessageCounterTextColor, unreadCountColor)
        mutedIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiMutedChannelIcon, mutedIcon)
        dateTextColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLastMessageDateTextColor, dateTextColor)
        statusIndicatorPendingIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorPendingIcon, statusIndicatorPendingIcon)
        statusIndicatorSentIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorSentIcon, statusIndicatorSentIcon)
        statusIndicatorDeliveredIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorDeliveredIcon, statusIndicatorDeliveredIcon)
        statusIndicatorReadIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorReadIcon, statusIndicatorReadIcon)
        statusIconSize = typedArray.getDimensionPixelSize(R.styleable.ChannelsListView_sceytUiStatusIndicatorSize, statusIconSize)
        emptyState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptyStateView, emptyState)
        emptySearchState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptySearchStateView, emptySearchState)
        loadingState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLoadingView, loadingState)
        onlineStatusColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiOnlineIndicatorColor, onlineStatusColor)
        dividerColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiDividerColor, dividerColor)
        enableDivider = typedArray.getBoolean(R.styleable.ChannelsListView_sceytUiEnableDivider, enableDivider)
        popupStyle = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiPopupStyle, popupStyle)
        return this
    }
}