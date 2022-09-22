package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.sceyt.sceytchatuikit.R

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

    @DrawableRes
    var indicatorPendingIcon: Int = R.drawable.sceyt_ic_status_not_sent

    @DrawableRes
    var indicatorSentIcon: Int = R.drawable.sceyt_ic_status_on_server

    @DrawableRes
    var indicatorDeliveredIcon: Int = R.drawable.sceyt_ic_status_delivered

    @DrawableRes
    var indicatorReadIcon: Int = R.drawable.sceyt_ic_status_read

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_channel_list_empty_state

    @LayoutRes
    var emptySearchState: Int = R.layout.sceyt_search_channels_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_channels_page_loading_state

    var enableDivider = true

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
        indicatorPendingIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorPendingIcon, indicatorPendingIcon)
        indicatorSentIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorSentIcon, indicatorSentIcon)
        indicatorDeliveredIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorDeliveredIcon, indicatorDeliveredIcon)
        indicatorReadIcon = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiIndicatorReadIcon, indicatorReadIcon)
        emptyState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptyStateView, emptyState)
        emptySearchState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptySearchStateView, emptySearchState)
        loadingState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLoadingView, loadingState)
        onlineStatusColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiOnlineIndicatorColor, onlineStatusColor)
        dividerColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiDividerColor, dividerColor)
        dividerColor = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiDividerColor, dividerColor)
        enableDivider = typedArray.getBoolean(R.styleable.ChannelsListView_sceytUiEnableDivider, enableDivider)
        return this
    }
}