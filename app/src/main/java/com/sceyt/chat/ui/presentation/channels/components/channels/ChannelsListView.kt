package com.sceyt.chat.ui.presentation.channels.components.channels

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extencions.getCompatColor
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.channels.components.PageState
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var channelsRV: ChannelsRV
    private val layoutInflater by lazy { LayoutInflater.from(context) }
    private var loadingStateView: View? = null
    private var emptyStateView: View? = null
    private var emptySearchStateView: View? = null

    init {
        setBackgroundColor(context.getCompatColor(R.color.colorBackground))

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelsListView)
            ChannelStyle.updateWithAttributes(a)
            a.recycle()
        }

        channelsRV = ChannelsRV(context)
        addView(channelsRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addLoadingState()
        addEmptySearchState()
        addEmptyState()
    }

    private fun addLoadingState() {
        loadingStateView = layoutInflater.inflate(ChannelStyle.loadingState, this, false).apply {
            isVisible = false
            addView(this)
        }
    }

    private fun addEmptyState() {
        emptyStateView = LayoutInflater.from(context).inflate(ChannelStyle.emptyState, this, false).apply {
            isVisible = false
            addView(this)
        }
    }

    private fun addEmptySearchState() {
        emptySearchStateView = layoutInflater.inflate(ChannelStyle.emptySearchState, this, false).apply {
            isVisible = false
            addView(this)
        }
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

    fun updateState(it: PageState) {
        when {
            it.isEmpty -> {
                emptyStateView?.isVisible = !it.isSearch
                emptySearchStateView?.isVisible = it.isSearch
                loadingStateView?.isVisible = false
            }
            it.isLoading && !it.isLoadingMore && !it.isSearch-> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = true
            }
            else -> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }
        }
    }
}