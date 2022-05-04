package com.sceyt.chat.ui.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import com.sceyt.chat.ui.R

object ChannelStyle {
    @ColorRes
    var titleColor: Int = R.color.colorFontDark

    @ColorRes
    var lastMessageTextColor: Int = R.color.colorFontGray

    @ColorRes
    var unreadCountColor: Int = R.color.colorAccent

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_ui_channel_list_empty_state

    @LayoutRes
    var emptySearchState: Int = R.layout.sceyt_ui_search_channels_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_ui_loading_state

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
        emptyState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptyStateView, emptyState)
        emptySearchState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiEmptySearchStateView, emptySearchState)
        loadingState = typedArray.getResourceId(R.styleable.ChannelsListView_sceytUiLoadingView, loadingState)
        return this
    }
}