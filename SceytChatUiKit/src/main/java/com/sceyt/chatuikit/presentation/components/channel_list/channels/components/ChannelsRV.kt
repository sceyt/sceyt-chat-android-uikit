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
        defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var mAdapter: ChannelsAdapter? = null
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
        val adapter = mAdapter ?: return
        if (isLastItemDisplaying() && adapter.itemCount != 0)
            reachToEndListener?.invoke(adapter.getSkip(), adapter.getChannels().lastOrNull()?.channel)
    }

    fun setData(channels: List<ChannelListItem>) {
        if (mAdapter == null) {
            adapter = ChannelsAdapter(SyncArrayList(channels), viewHolderFactory)
                .also { mAdapter = it }
        } else {
            mAdapter?.notifyUpdate(channels, this)
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
        return (mAdapter?.getSkip() ?: return true) == 0
    }

    override fun getAdapter(): ChannelsAdapter? {
        return mAdapter
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        if (mAdapter == null)
            setData(channels)
        else
            mAdapter?.addList(channels)
    }

    fun deleteChannel(id: Long) {
        mAdapter?.deleteChannel(id)
    }

    fun getChannels(): List<ChannelListItem.ChannelItem>? {
        return mAdapter?.getChannels()
    }

    fun getData(): List<ChannelListItem>? {
        return mAdapter?.getData()
    }

    fun getChannelIndexed(channelId: Long): Pair<Int, ChannelListItem.ChannelItem>? {
        return mAdapter?.getData()?.findIndexed { it is ChannelListItem.ChannelItem && it.channel.id == channelId }?.let {
            return@let Pair(it.first, it.second as ChannelListItem.ChannelItem)
        }
    }

    fun getDirectChannelByUserIdIndexed(userId: String): Pair<Int, ChannelListItem.ChannelItem>? {
        return mAdapter?.getData()?.findIndexed {
            it is ChannelListItem.ChannelItem && it.channel.isDirect()
                    && it.channel.getPeer()?.id == userId
        }?.let {
            return@let Pair(it.first, it.second as ChannelListItem.ChannelItem)
        }
    }

    /** Call this function to customise ChannelViewHolderFactory and set your own.
     * Note: Call this function before initialising channels adapter.*/
    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        check(mAdapter == null) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
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
        sortAndUpdate(sortChannelsBy, mAdapter?.getData() ?: return)
    }

    fun sortByAndSetNewData(sortChannelsBy: ChannelListOrder, data: List<ChannelListItem>) {
        sortAndUpdate(sortChannelsBy, data)
    }

    fun hideLoadingMore() {
        mAdapter?.removeLoading()
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