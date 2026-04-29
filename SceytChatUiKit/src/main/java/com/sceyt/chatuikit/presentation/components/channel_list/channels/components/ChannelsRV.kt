package com.sceyt.chatuikit.presentation.components.channel_list.channels.components

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.isFirstItemDisplaying
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.styles.channel.ChannelListViewStyle

class ChannelsRV @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var channelListStyle: ChannelListViewStyle
    private var channelsAdapter: ChannelsAdapter? = null
    private var reachToEndListener: ((offset: Int, lastChannel: SceytChannel?) -> Unit)? = null
    private var channelViewHolderFactory: ChannelViewHolderFactory by lazyVar {
        ChannelViewHolderFactory(context, channelListStyle.itemStyle)
    }

    init {
        init()
    }

    private fun init() {
        setHasFixedSize(true)
        setItemViewCacheSize(10)
        itemAnimator = DefaultItemAnimator().apply {
            moveDuration = 100
            changeDuration = 0
        }
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
            reachToEndListener?.invoke(
                adapter.getSkip(),
                adapter.getChannels().lastOrNull()?.channel
            )
    }

    fun setData(scope: LifecycleCoroutineScope, channels: List<ChannelListItem>) = post {
        if (channelsAdapter == null) {
            adapter = ChannelsAdapter(scope, channelViewHolderFactory).also {
                channelsAdapter = it
            }
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

    @Suppress("unused")
    fun getChannelItemIndexed(channelId: Long): Pair<Int, ChannelListItem.ChannelItem>? {
        return channelsAdapter?.currentList?.findIndexed {
            it is ChannelListItem.ChannelItem && it.channel.id == channelId
        }?.let { (index, item) ->
            index to item as ChannelListItem.ChannelItem
        }
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
        channelViewHolderFactory = factory
    }

    fun setReachToEndListeners(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        reachToEndListener = listener
    }

    fun setAttachDetachListeners(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        channelViewHolderFactory.setChannelAttachDetachListener(listener)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        channelViewHolderFactory.setChannelListener(listener)
    }

    @Suppress("unused")
    fun getViewHolderFactory() = channelViewHolderFactory

    internal fun setStyle(style: ChannelListViewStyle) {
        channelListStyle = style
    }
}