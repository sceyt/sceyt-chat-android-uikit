package com.sceyt.sceytchatuikit.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.extensions.addRVScrollListener
import com.sceyt.sceytchatuikit.extensions.awaitAnimationEnd
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.isFirstItemDisplaying
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.maybeComponentActivity
import com.sceyt.sceytchatuikit.presentation.common.SyncArrayList
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsItemComparatorBy
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChannelsRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: ChannelsAdapter
    private var richToEndListener: ((offset: Int, lastChannel: SceytChannel?) -> Unit)? = null
    private var viewHolderFactory = ChannelViewHolderFactory(context)

    init {
        init()
        ChannelViewHolderFactory.cacheViews(context)
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
                checkRichToEnd()
            }
        }
    }

    private fun checkRichToEnd() {
        if (isLastItemDisplaying() && mAdapter.itemCount != 0)
            richToEndListener?.invoke(mAdapter.getSkip(), mAdapter.getChannels().lastOrNull()?.channel)
    }

    fun setData(channels: List<ChannelListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = ChannelsAdapter(SyncArrayList(channels), viewHolderFactory)
                .also { mAdapter = it }
        } else {
            mAdapter.notifyUpdate(channels, this)
            awaitAnimationEnd {
                if (isFirstItemDisplaying())
                    scrollToPosition(0)
            }

            context.maybeComponentActivity()?.let {
                it.lifecycleScope.launch {
                    it.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        delay(500)
                        checkRichToEnd()
                    }
                }
            }
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
        if (::mAdapter.isInitialized)
            mAdapter.deleteChannel(id)
    }

    fun getChannels(): List<ChannelListItem.ChannelItem>? {
        return if (::mAdapter.isInitialized) mAdapter.getChannels() else null
    }

    fun getData(): List<ChannelListItem>? {
        return if (::mAdapter.isInitialized) mAdapter.getData() else null
    }

    fun getChannelIndexed(channelId: Long): Pair<Int, ChannelListItem.ChannelItem>? {
        return if (::mAdapter.isInitialized)
            mAdapter.getData().findIndexed { it is ChannelListItem.ChannelItem && it.channel.id == channelId }?.let {
                return@let Pair(it.first, it.second as ChannelListItem.ChannelItem)
            }
        else null
    }

    fun getDirectChannelByUserIdIndexed(userId: String): Pair<Int, ChannelListItem.ChannelItem>? {
        return if (::mAdapter.isInitialized)
            mAdapter.getData().findIndexed {
                it is ChannelListItem.ChannelItem && it.channel.isDirect()
                        && it.channel.getFirstMember()?.id == userId
            }?.let {
                return@let Pair(it.first, it.second as ChannelListItem.ChannelItem)
            }
        else null
    }

    /** Call this function to customise ChannelViewHolderFactory and set your own.
     * Note: Call this function before initialising channels adapter.*/
    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        check(::mAdapter.isInitialized.not()) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun setRichToEndListeners(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        richToEndListener = listener
    }

    fun setAttachDetachListeners(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        viewHolderFactory.setChannelAttachDetachListener(listener)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        viewHolderFactory.setChannelListener(listener)
    }

    fun sortBy(sortChannelsBy: SceytKitConfig.ChannelSortType) {
        sortAndUpdate(sortChannelsBy, mAdapter.getData())
    }

    fun sortByAndSetNewData(sortChannelsBy: SceytKitConfig.ChannelSortType, data: List<ChannelListItem>) {
        sortAndUpdate(sortChannelsBy, data)
    }

    fun hideLoadingMore() {
        if (::mAdapter.isInitialized) mAdapter.removeLoading()
    }

    fun getViewHolderFactory() = viewHolderFactory

    private fun sortAndUpdate(sortChannelsBy: SceytKitConfig.ChannelSortType, data: List<ChannelListItem>) {
        val sortedList = data.sortedWith(ChannelsItemComparatorBy(sortChannelsBy))
        awaitAnimationEnd {
            post { setData(sortedList) }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ChannelViewHolderFactory.clearCache()
    }
}