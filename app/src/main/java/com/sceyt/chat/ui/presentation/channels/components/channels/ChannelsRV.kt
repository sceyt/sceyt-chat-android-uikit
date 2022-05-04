package com.sceyt.chat.ui.presentation.channels.components.channels

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelsAdapter

class ChannelsRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: ChannelsAdapter
    private var mRichToEndListener: ((offset: Int) -> Unit)? = null

    init {
        init()
        ChannelViewHolderFactory.cashViews(context)
    }

    private fun init() {
        clipToPadding = false
        setHasFixedSize(true)
        setItemViewCacheSize(10)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        // edgeEffectFactory = BounceEdgeEffectFactory()
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, _: Int ->
                if (isLastItemDisplaying() && mAdapter.itemCount != 0)
                    mRichToEndListener?.invoke(mAdapter.getSkip())
            }
        }
    }

    fun setData(channels: List<ChannelListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = ChannelsAdapter(channels as ArrayList<ChannelListItem>).also { mAdapter = it }
        } else
            mAdapter.notifyUpdate(channels)
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    fun setRichToEndListeners(listener: (offset: Int) -> Unit) {
        mRichToEndListener = listener
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(channels)
        else
            mAdapter.addList(channels as MutableList<ChannelListItem>)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ChannelViewHolderFactory.clearCash()
    }
}