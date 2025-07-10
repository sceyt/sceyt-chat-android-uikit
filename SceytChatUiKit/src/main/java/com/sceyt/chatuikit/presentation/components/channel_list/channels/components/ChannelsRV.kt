package com.sceyt.chatuikit.presentation.components.channel_list.channels.components

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.isFirstItemDisplaying
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsItemComparatorBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.styles.ChannelListViewStyle

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

    fun setData(scope: LifecycleCoroutineScope, channels: List<ChannelListItem>) = post {
        if (channelsAdapter == null) {
            adapter = ChannelsAdapter(scope, viewHolderFactory)
                .also { channelsAdapter = it }
            channelsAdapter?.notifyUpdate(channels)
        } else {
            val needScrollUp = isFirstItemDisplaying()
            channelsAdapter?.notifyUpdate(channels) {
                awaitAnimationEnd {
                    if (needScrollUp)
                        scrollToPosition(0)
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

    fun addNewChannels(scope: LifecycleCoroutineScope, channels: List<ChannelListItem>) {
        if (channelsAdapter == null)
            setData(scope, channels)
        else
            channelsAdapter?.addList(channels)
    }

    fun deleteChannel(id: Long, commitCallback: (() -> Unit)? = null) {
        channelsAdapter?.deleteChannel(id, commitCallback)
    }

    fun getChannels(): List<ChannelListItem.ChannelItem>? {
        return channelsAdapter?.getChannels()
    }

    fun getData(): List<ChannelListItem>? {
        return channelsAdapter?.currentList
    }

    fun getChannelItem(channelId: Long): ChannelListItem.ChannelItem? {
        return channelsAdapter?.currentList?.firstOrNull {
            it is ChannelListItem.ChannelItem && it.channel.id == channelId
        } as? ChannelListItem.ChannelItem
    }

    fun updateChannel(
            predicate: (ChannelListItem) -> Boolean,
            newItem: ChannelListItem,
            payloads: Any? = null,
            commitCallback: (() -> Unit)? = null,
    ) {
        post {
            channelsAdapter?.updateChannel(
                predicate,
                newItem,
                payloads,
                commitCallback
            )
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

    fun sortBy(
            scope: LifecycleCoroutineScope,
            sortChannelsBy: ChannelListOrder = SceytChatUIKit.config.channelListOrder,
    ) {
        sortAndUpdate(scope, sortChannelsBy, channelsAdapter?.currentList ?: return)
    }

    fun sortByAndSetNewData(
            scope: LifecycleCoroutineScope,
            sortChannelsBy: ChannelListOrder,
            data: List<ChannelListItem>,
    ) {
        sortAndUpdate(scope, sortChannelsBy, data)
    }

    fun hideLoadingMore() {
        channelsAdapter?.removeLoading()
    }

    fun getViewHolderFactory() = viewHolderFactory

    internal fun setStyle(channelStyle: ChannelListViewStyle) {
        viewHolderFactory.setStyle(channelStyle)
    }

    private fun sortAndUpdate(
            scope: LifecycleCoroutineScope,
            sortChannelsBy: ChannelListOrder,
            data: List<ChannelListItem>,
    ) {
        val sortedList = data.sortedWith(ChannelsItemComparatorBy(sortChannelsBy))
        awaitAnimationEnd {
            post { setData(scope, sortedList) }
        }
    }
}