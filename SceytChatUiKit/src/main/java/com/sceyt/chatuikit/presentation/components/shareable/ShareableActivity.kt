package com.sceyt.chatuikit.presentation.components.shareable

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.isPeerBlocked
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelListState
import com.sceyt.chatuikit.presentation.components.shareable.adapter.ShareableChannelsAdapter
import com.sceyt.chatuikit.presentation.components.shareable.adapter.holders.ShareableChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.shareable.viewmodel.ShareableViewModel
import com.sceyt.chatuikit.presentation.components.shareable.viewmodel.ShareableViewModelFactory
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.share.ShareablePageStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class ShareableActivity<Style : ShareablePageStyle> : AppCompatActivity(),
    SceytKoinComponent {
    protected val shareableViewModel: ShareableViewModel by viewModels(factoryProducer = {
        provideViewModelFactory()
    })
    protected var channelsAdapter: ShareableChannelsAdapter? = null
    protected lateinit var style: Style
    protected val viewHolderFactory by lazy { provideViewHolderFactory() }
    protected var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        style = initStyle()
        initViewModel()
    }

    protected abstract fun initStyle(): Style

    private fun initViewModel() {
        shareableViewModel.state
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .onEach(::initStateUpdate)
            .launchIn(lifecycleScope)
    }

    protected open suspend fun initStateUpdate(state: ChannelListState) {
        val filteredChannels = filterOnlyAppropriateChannels(state.channels)
        val data = filteredChannels.map { ChannelListItem.ChannelItem(it) } +
                if (state.hasNext) listOf(ChannelListItem.LoadingMoreItem) else emptyList()
        setChannelsList(data)
    }

    protected open fun setChannelsList(data: List<ChannelListItem>) {
        val recyclerView = getRV() ?: return
        setSelectedItems(data)
        if (channelsAdapter == null || recyclerView.adapter !is ShareableChannelsAdapter) {
            channelsAdapter = ShareableChannelsAdapter(
                scope = lifecycleScope,
                viewHolderFactory = viewHolderFactory.also {
                    it.setChannelClickListener { _, item -> onChannelClick(item) }
                })

            with(recyclerView) {
                adapter = channelsAdapter
                layoutManager = LinearLayoutManager(this@ShareableActivity)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (adapter is ShareableChannelsAdapter && isLastItemDisplaying())
                            shareableViewModel.loadMoreChannels()
                    }
                })
            }
            channelsAdapter?.notifyUpdate(data)
        } else channelsAdapter?.notifyUpdate(data)
        updateEmptyState(data.isEmpty(), currentSearchQuery)
    }

    protected open fun filterOnlyAppropriateChannels(data: List<SceytChannel>): List<SceytChannel> {
        return data.filterNot { channel ->
            channel.isPeerDeleted() || channel.isPeerBlocked() ||
                    (channel.isPublic() && channel.userRole != RoleTypeEnum.Owner.value
                            && channel.userRole != RoleTypeEnum.Admin.value)
        }
    }

    private fun setSelectedItems(data: List<ChannelListItem>) {
        data.forEach {
            it.selected =
                it is ChannelListItem.ChannelItem && selectedChannels.contains(it.channel.id)
        }
    }

    protected open fun provideViewHolderFactory(): ShareableChannelViewHolderFactory {
        return ShareableChannelViewHolderFactory(this, style)
    }

    protected open fun provideViewModelFactory(): ShareableViewModelFactory {
        return ShareableViewModelFactory()
    }

    @CallSuper
    protected open fun onChannelClick(channelItem: ChannelListItem.ChannelItem): Boolean {
        var isAdded = false
        val channel = channelItem.channel
        if (selectedChannels.contains(channel.id)) {
            selectedChannels.remove(channel.id)
            channelsAdapter?.updateChannelSelectedState(false, channelItem)
        } else {
            if (selectedChannels.size < 5) {
                selectedChannels.add(channel.id)
                channelsAdapter?.updateChannelSelectedState(true, channelItem)
                isAdded = true
            } else customToastSnackBar(getString(R.string.sceyt_share_max_chats_count))
        }
        return isAdded
    }

    protected open fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        shareableViewModel.onSearchQueryChanged(query)
    }

    protected open fun updateEmptyState(isEmpty: Boolean, searchQuery: String = "") {
        val pageStateView = getPageStateView() ?: return
        if (isEmpty) {
            pageStateView.updateState(PageState.StateEmpty(searchQuery))
        } else {
            pageStateView.updateState(PageState.Nothing)
        }
    }

    protected open val selectedChannels get() = shareableViewModel.selectedChannels

    protected open fun finishSharingAction() {
        if (isTaskRoot) {
            packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        }

        super.finish()
    }

    protected open fun enableNext(): Boolean {
        return selectedChannels.isNotEmpty()
    }

    protected open fun getRV(): RecyclerView? {
        return null
    }

    protected open fun getPageStateView(): PageStateView? {
        return null
    }
}