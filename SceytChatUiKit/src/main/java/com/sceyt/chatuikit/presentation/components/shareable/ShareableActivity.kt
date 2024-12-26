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
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModel
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModelFactory
import com.sceyt.chatuikit.presentation.components.shareable.adapter.ShareableChannelsAdapter
import com.sceyt.chatuikit.presentation.components.shareable.adapter.holders.ShareableChannelViewHolderFactory
import com.sceyt.chatuikit.styles.ShareablePageStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class ShareableActivity<Style : ShareablePageStyle> : AppCompatActivity(), SceytKoinComponent {
    protected val channelsViewModel: ChannelsViewModel by viewModels(factoryProducer = {
        provideChannelsViewModelFactory()
    })
    protected var channelsAdapter: ShareableChannelsAdapter? = null
    protected lateinit var style: Style
    protected val viewHolderFactory by lazy { provideViewHolderFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        style = initStyle()
        initViewModel()
        channelsViewModel.getChannels(0)
    }

    protected abstract fun initStyle(): Style

    private fun initViewModel() {
        channelsViewModel.loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleScope)
    }

    protected open suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        lifecycleScope.launch {
            if (response is PaginationResponse.DBResponse)
                initPaginationDbResponse(response)
        }
    }

    protected open suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        val filteredData = filterOnlyAppropriateChannels(response.data)
        val data = channelsViewModel.mapToChannelItem(data = filteredData,
            hasNext = response.hasNext,
            includeDirectChannelsWithDeletedPeers = false)
        if (response.offset == 0) {
            setChannelsList(data)
        } else addNewChannels(data)
    }

    protected open fun setChannelsList(data: List<ChannelListItem>) {
        lifecycleScope.launch {
            lifecycle.withResumed {
                val rv = getRV() ?: return@withResumed
                setSelectedItems(data)
                if (channelsAdapter == null || rv.adapter !is ShareableChannelsAdapter) {
                    channelsAdapter = ShareableChannelsAdapter(data.toMutableList(), viewHolderFactory.also {
                        it.setChannelClickListener(::onChannelClick)
                    }).also { channelsAdapter = it }
                    with(rv) {
                        adapter = channelsAdapter
                        layoutManager = LinearLayoutManager(this@ShareableActivity)
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)
                                if (adapter is ShareableChannelsAdapter && isLastItemDisplaying() && channelsViewModel.canLoadNext())
                                    channelsViewModel.getChannels(channelsAdapter?.getSkip()
                                            ?: 0, channelsViewModel.searchQuery,
                                        LoadKeyData(value = channelsAdapter?.getChannels()?.lastOrNull()?.channel?.id
                                                ?: 0))
                            }
                        })
                    }
                } else channelsAdapter?.notifyUpdate(data, rv)
            }
        }
    }

    protected open fun addNewChannels(data: List<ChannelListItem>) {
        setSelectedItems(data)
        channelsAdapter?.addList(data as MutableList<ChannelListItem>)
    }

    protected open fun filterOnlyAppropriateChannels(data: List<SceytChannel>): List<SceytChannel> {
        val filtered = data.filter {
            ((it.isPublic() && (it.userRole != RoleTypeEnum.Owner.value &&
                    it.userRole != RoleTypeEnum.Admin.value)) || ((it.isPeerDeleted() || it.isPeerBlocked())))
        }

        return data.minus(filtered.toSet())
    }

    private fun setSelectedItems(data: List<ChannelListItem>) {
        data.forEach {
            it.selected = it is ChannelListItem.ChannelItem && selectedChannels.contains(it.channel.id)
        }
    }

    protected open fun provideViewHolderFactory(): ShareableChannelViewHolderFactory {
        return ShareableChannelViewHolderFactory(this, style)
    }

    protected open fun provideChannelsViewModelFactory(): ChannelsViewModelFactory {
        return ChannelsViewModelFactory()
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
        channelsViewModel.getChannels(0, query)
    }

    protected open val selectedChannels get() = channelsViewModel.selectedChannels

    protected open fun finishSharingAction() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null)
            startActivity(intent)

        super.finish()
    }

    protected open fun enableNext(): Boolean {
        return selectedChannels.isNotEmpty()
    }

    protected open fun getRV(): RecyclerView? {
        return null
    }
}