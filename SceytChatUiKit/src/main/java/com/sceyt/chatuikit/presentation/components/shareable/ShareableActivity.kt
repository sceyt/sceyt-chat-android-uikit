package com.sceyt.chatuikit.presentation.components.shareable

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.isPeerBlocked
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.shareable.adapter.ShareableChannelsAdapter
import com.sceyt.chatuikit.presentation.components.shareable.adapter.holders.ShareableChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.shareable.viewmodel.ShareableViewModel
import com.sceyt.chatuikit.presentation.components.shareable.viewmodel.ShareableViewModelFactory
import com.sceyt.chatuikit.styles.share.ShareablePageStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class ShareableActivity<Style : ShareablePageStyle> : AppCompatActivity(),
    SceytKoinComponent {
    protected val shareableViewModel: ShareableViewModel by viewModels(factoryProducer = {
        provideViewModelFactory()
    })
    protected var channelsAdapter: ShareableChannelsAdapter? = null
    protected lateinit var style: Style
    protected val viewHolderFactory by lazy { provideViewHolderFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        style = initStyle()
        initViewModel()
        shareableViewModel.getChannels(0)
    }

    protected abstract fun initStyle(): Style

    private fun initViewModel() {
        shareableViewModel.loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleScope)
    }

    protected open suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        if (response is PaginationResponse.DBResponse)
            initPaginationDbResponse(response)
    }

    protected open suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        val filteredData = filterOnlyAppropriateChannels(response.data)
        val data = shareableViewModel.mapToChannelItem(
            data = filteredData,
            hasNext = response.hasNext,
            includeDirectChannelsWithDeletedPeers = false
        )
        if (response.offset == 0) {
            setChannelsList(data)
        } else addNewChannels(data)
    }

    protected open fun setChannelsList(data: List<ChannelListItem>) {
        lifecycleScope.launch {
            lifecycle.withResumed {
                val recyclerView = getRV() ?: return@withResumed
                setSelectedItems(data)
                if (channelsAdapter == null || recyclerView.adapter !is ShareableChannelsAdapter) {
                    channelsAdapter = ShareableChannelsAdapter(
                        channels = data.toMutableList(),
                        viewHolderFactory = viewHolderFactory.also {
                            it.setChannelClickListener { view, item ->
                                onChannelClick(item)
                            }
                        })
                        .also { adapter ->
                            channelsAdapter = adapter
                        }

                    with(recyclerView) {
                        adapter = channelsAdapter
                        layoutManager = LinearLayoutManager(this@ShareableActivity)
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)
                                if (adapter is ShareableChannelsAdapter && isLastItemDisplaying() && shareableViewModel.canLoadNext())
                                    shareableViewModel.getChannels(
                                        offset = channelsAdapter?.getSkip() ?: 0,
                                        query = shareableViewModel.searchQuery,
                                        loadKey = LoadKeyData(
                                            value = channelsAdapter?.getChannels()
                                                ?.lastOrNull()?.channel?.id ?: 0
                                        )
                                    )
                            }
                        })
                    }
                } else channelsAdapter?.notifyUpdate(data, recyclerView)
            }
        }
    }

    protected open fun addNewChannels(data: List<ChannelListItem>) {
        setSelectedItems(data)
        channelsAdapter?.addList(data as MutableList<ChannelListItem>)
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
        shareableViewModel.getChannels(0, query)
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
}