package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private val viewHolderFactory = ChannelViewHolderFactory(context)

    init {
        init()
        ChannelViewHolderFactory.cashViews(context)
    }

    private fun init() {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ChannelViewHolderFactory.clearCash()
    }
}