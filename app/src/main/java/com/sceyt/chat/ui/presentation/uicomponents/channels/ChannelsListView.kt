package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelListeners
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.BindingUtil

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var channelsRV: ChannelsRV
    private var pageStateView: PageStateView? = null

    init {
        setBackgroundColor(context.getCompatColor(R.color.colorBackground))
        BindingUtil.themedBackgroundColor(this, R.color.colorBackground)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelsListView)
            ChannelStyle.updateWithAttributes(a)
            a.recycle()
        }

        channelsRV = ChannelsRV(context)
        addView(channelsRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addView(PageStateView(context).also {
            pageStateView = it
            it.setLoadingStateView(ChannelStyle.loadingState)
            it.setEmptyStateView(ChannelStyle.emptyState)
            it.setEmptySearchStateView(ChannelStyle.emptySearchState)
        })
    }

    fun setReachToEndListener(listener: (offset: Int) -> Unit) {
        channelsRV.setRichToEndListeners(listener)
    }

    fun setChannelsList(channels: List<ChannelListItem>) {
        channelsRV.setData(channels)
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    fun updateState(state: BaseViewModel.PageState) {
        pageStateView?.updateState(state, channelsRV.isEmpty())
    }

    fun setChannelListener(listener: ChannelListeners) {
        channelsRV.setChannelListener(listener)
    }
}