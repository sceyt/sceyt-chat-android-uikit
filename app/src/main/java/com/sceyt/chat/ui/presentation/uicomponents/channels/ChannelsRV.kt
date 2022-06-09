package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.isFirstItemDisplaying
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelsComparatorBy
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

class ChannelsRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: ChannelsAdapter
    private var richToEndListener: ((offset: Int) -> Unit)? = null
    private var viewHolderFactory = ChannelViewHolderFactory(context)

    init {
        init()
        ChannelViewHolderFactory.cashViews(context)
    }

    private fun init() {
        setHasFixedSize(true)
        setItemViewCacheSize(10)
        itemAnimator = DefaultItemAnimator()
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        // edgeEffectFactory = BounceEdgeEffectFactory()
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, _: Int ->
                if (isLastItemDisplaying() && mAdapter.itemCount != 0)
                    richToEndListener?.invoke(mAdapter.getSkip())
            }
        }
    }

    fun setData(channels: List<ChannelListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = ChannelsAdapter(channels as ArrayList<ChannelListItem>, viewHolderFactory)
                .also { mAdapter = it }
        } else {
            mAdapter.notifyUpdate(channels)
            if (isFirstItemDisplaying())
                scrollToPosition(0)
        }
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    override fun getAdapter(): ChannelsAdapter? {
        return if (::mAdapter.isInitialized) {
            mAdapter
        } else null
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(channels)
        else
            mAdapter.addList(channels as MutableList<ChannelListItem>)
    }

    fun deleteChannel(id: Long) {
        mAdapter.deleteChannel(id)
    }

    fun getChannels(): List<ChannelListItem.ChannelItem>? {
        return if (::mAdapter.isInitialized) mAdapter.getChannels() else null
    }

    /** Call this function to customise ChannelViewHolderFactory and set your own.
     * Note: Call this function before initialising channels adapter.*/
    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        check(::mAdapter.isInitialized.not()) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun setRichToEndListeners(listener: (offset: Int) -> Unit) {
        richToEndListener = listener
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        viewHolderFactory.setChannelListener(listener)
    }

    fun sortBy(sortChannelsBy: SceytUIKitConfig.ChannelSortType) {
        val hasLoading = mAdapter.getData().findLast { it is ChannelListItem.LoadingMoreItem } != null
        val sortedList = ArrayList(mAdapter.getChannels().map { it.channel })
            .sortedWith(ChannelsComparatorBy(sortChannelsBy))

        val newList: ArrayList<ChannelListItem> = ArrayList(sortedList.map { ChannelListItem.ChannelItem(it) })
        if (hasLoading)
            newList.add(ChannelListItem.LoadingMoreItem)
        setData(newList)
    }

    fun updateMuteState(muted: Boolean, channelId: Long) {
        mAdapter.getData().forEachIndexed { index, item ->
            if (item is ChannelListItem.ChannelItem && item.channel.id == channelId) {
                item.channel.muted = muted
                mAdapter.notifyItemChanged(index)
                return
            }
        }
    }

    fun updateChannel(channel: SceytChannel) {
        mAdapter.getData().forEachIndexed { index, item ->
            if (item is ChannelListItem.ChannelItem && item.channel.id == channel.id) {
                item.channel = channel
                mAdapter.notifyItemChanged(index)
                return
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ChannelViewHolderFactory.clearCash()
    }
}