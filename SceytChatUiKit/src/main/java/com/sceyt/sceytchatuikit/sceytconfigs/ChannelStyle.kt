package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.pxToDp

object ChannelStyle {
    @ColorRes
    var titleColor: Int = R.color.sceyt_color_text_themed

    @ColorRes
    var lastMessageTextColor: Int = R.color.sceyt_color_gray_400

    @ColorRes
    var unreadCountColor: Int = R.color.sceyt_color_accent

    @ColorRes
    var onlineStatusColor: Int = R.color.sceyt_color_green

    @ColorRes
    var dividerColor: Int = R.color.sceyt_color_divider

    @DrawableRes
    var mutedIcon: Int = R.drawable.sceyt_ic_muted

    @ColorRes
    var dateTextColor: Int = R.color.sceyt_color_gray_400

    @DrawableRes
    var statusIndicatorPendingIcon: Int = R.drawable.sceyt_ic_status_not_sent

    @DrawableRes
    var statusIndicatorSentIcon: Int = R.drawable.sceyt_ic_status_on_server

    @DrawableRes
    var statusIndicatorDeliveredIcon: Int = R.drawable.sceyt_ic_status_delivered

    @DrawableRes
    var statusIndicatorReadIcon: Int = R.drawable.sceyt_ic_status_read

    var statusIconSize: Int = pxToDp(16f).toInt()

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_channel_list_empty_state

    @LayoutRes
    var emptySearchState: Int = R.layout.sceyt_search_channels_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_channels_page_loading_state

    var enableDivider = true

    @StyleRes
    var popupStyle: Int = R.style.SceytPopupMenuStyle

    /*internal constructor(context: Context) : this(
        titleColor = context.getCompatColor(R.color.colorFontDark),
        lastMessageTextColor = context.getCompatColor(R.color.colorFontGray),
        unreadCountColor = context.getCompatColor(R.color.colorAccent)
    )*/

    /* internal constructor(context: Context, typedArray: TypedArray) : this(
         titleColor = typedArray.getResourceId(
             R.styleable.ChannelListView_sceytUiChannelTitleTextColor,
             R.color.colorFontDark
         ),
         lastMessageTextColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiLastMessageTextColor,
             context.getCompatColor(R.color.colorFontGray)
         ),
         unreadCountColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor,
             context.getCompatColor(R.color.colorAccent)
         )
     )*/


    internal fun updateWithAttributes(typedArray: TypedArray): ChannelStyle {
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