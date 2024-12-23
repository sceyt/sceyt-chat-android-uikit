package com.sceyt.chatuikit.presentation.components.channel_list.channels.components

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.isFirstItemDisplaying
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsItemComparatorBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.styles.ChannelListViewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChannelsRV @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private var channelsAdapter: ChannelsAdapter? = null
    private var reachToEndListener: ((offset: Int, lastChannel: SceytChannel?) -> Unit)? = null
    private var viewHolderFactory = ChannelViewHolderFactory(context)

    init {
        init()
    }

    private fun init() {
        setHasFixedSize(true)
        setItemViewCacheSize(10)
        itemAnimator = null
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        // edgeEffectFactory = BounceEdgeEffectFactory()
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, _: Int ->
                checkReachToEnd()
            }
        }
    }

    private fun checkReachToEnd() {
        val adapter = channelsAdapter ?: return
        if (isLastItemDisplaying() && adapter.itemCount != 0)
            reachToEndListener?.invoke(adapter.getSkip(), adapter.getChannels().lastOrNull()?.channel)
    }

    fun setData(channels: List<ChannelListItem>) {
        if (channelsAdapter == null) {
            adapter = ChannelsAdapter(SyncArrayList(channels), viewHolderFactory)
                .also { channelsAdapter = it }
        } else {
            channelsAdapter?.notifyUpdate(channels, this)
            awaitAnimationEnd {
                if (isFirstItemDisplaying())
                    scrollToPosition(0)
            }

            context.maybeComponentActivity()?.let {
                it.lifecycleScope.launch {
                    it.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        delay(500)
                        checkReachToEnd()
                    }
                }
            }
        }
    }

    fun isEmpty(): Boolean {
        return (channelsAdapter?.getSkip() ?: return true) == 0
    }

    override fun getAdapter(): ChannelsAdapter? {
        return channelsAdapter
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        if (channelsAdapter == null)
            setData(channels)
        else
            channelsAdapter?.addList(channels)
    }

    fun deleteChannel(id: Long): Boolean {
        return channelsAdapter?.deleteChannel(id) ?: false
    }

    fun getChannels(): List<ChannelListItem.ChannelItem>? {
        return channelsAdapter?.getChannels()
    }

    fun getData(): List<ChannelListItem>? {
        return channelsAdapter?.getData()
    }

    fun getChannelIndexed(channelId: Long): Pair<Int, ChannelListItem.ChannelItem>? {
        return channelsAdapter?.getData()?.findIndexed {
            it is ChannelListItem.ChannelItem && it.channel.id == channelId
        }?.let {
            Pair(it.first, it.second as ChannelListItem.ChannelItem)
        }
    }

    /** Call this function to customise ChannelViewHolderFactory and set your own.
     * Note: Call this function before initialising channels adapter.*/
    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        check(channelsAdapter == null) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun setReachToEndListeners(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        reachToEndListener = listener
    }

    fun setAttachDetachListeners(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        viewHolderFactory.setChannelAttachDetachListener(listener)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        viewHolderFactory.setChannelListener(listener)
    }

    fun sortBy(sortChannelsBy: ChannelListOrder = SceytChatUIKit.config.channelListOrder) {
        sortAndUpdate(sortChannelsBy, channelsAdapter?.getData() ?: return)
    }

    fun sortByAndSetNewData(sortChannelsBy: ChannelListOrder, data: List<ChannelListItem>) {
        sortAndUpdate(sortChannelsBy, data)
    }

    fun hideLoadingMore() {
        channelsAdapter?.removeLoading()
    }

    fun getViewHolderFactory() = viewHolderFactory

    internal fun setStyle(channelStyle: ChannelListViewStyle) {
        viewHolderFactory.setStyle(channelStyle)
    }

    private fun sortAndUpdate(sortChannelsBy: ChannelListOrder, data: List<ChannelListItem>) {
        val sortedList = data.sortedWith(ChannelsItemComparatorBy(sortChannelsBy))
        awaitAnimationEnd {
            post { setData(sortedList) }
        }
    }
}