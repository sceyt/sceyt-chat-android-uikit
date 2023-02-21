package com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.presentation.common.getMyRole
import com.sceyt.sceytchatuikit.presentation.common.isPeerBlocked
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.adapter.ShareableChannelsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.adapter.viewholders.ShareableChannelViewHolderFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

open class SceytShareableActivity : AppCompatActivity(), SceytKoinComponent {
    protected val channelsViewModel: ChannelsViewModel by viewModels()
    protected var channelsAdapter: ShareableChannelsAdapter? = null
    private val viewHolderFactory by lazy { ShareableChannelViewHolderFactory(this) }
    protected val selectedChannels = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        channelsViewModel.getChannels(0)
    }

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
        var data = channelsViewModel.mapToChannelItem(data = response.data, hasNext = response.hasNext)
        data = filterOnlyAppropriateChannels(data)
        if (response.offset == 0) {
            setChannelsList(data)
        } else addNewChannels(data)
    }

    protected open fun setChannelsList(data: List<ChannelListItem>) {
        val rv = getRV() ?: return
        setSelectedItems(data)
        if (channelsAdapter == null || rv.adapter !is ShareableChannelsAdapter) {
            channelsAdapter = ShareableChannelsAdapter(data.toMutableList(), viewHolderFactory.also {
                it.setChannelClickListener(::onChannelClick)
            }).also { channelsAdapter = it }
            with(rv) {
                adapter = channelsAdapter
                layoutManager = LinearLayoutManager(this@SceytShareableActivity)
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

    protected open fun addNewChannels(data: List<ChannelListItem>) {
        setSelectedItems(data)
        channelsAdapter?.addList(data as MutableList<ChannelListItem>)
    }

    protected fun filterOnlyAppropriateChannels(data: List<ChannelListItem>): List<ChannelListItem> {
        val filtered = data.filter {
            (it is ChannelListItem.ChannelItem && (it.channel.channelType == ChannelTypeEnum.Public &&
                    (it.channel.getMyRole()?.name != RoleTypeEnum.Owner.toString() && it.channel.getMyRole()?.name != RoleTypeEnum.Admin.toString()))
                    || (it is ChannelListItem.ChannelItem && (it.channel.isPeerDeleted() || it.channel.isPeerBlocked())))
        }

        return data.minus(filtered.toSet())
    }

    private fun setSelectedItems(data: List<ChannelListItem>) {
        data.forEach {
            it.selected = it is ChannelListItem.ChannelItem && selectedChannels.contains(it.channel.id)
        }
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

    open fun finishSharingAction() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null)
            startActivity(intent)

        super.finish()
    }

    protected open fun enableNext(): Boolean {
        return selectedChannels.isNotEmpty()
    }

    open fun getRV(): RecyclerView? {
        return null
    }
}