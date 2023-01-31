package com.sceyt.sceytchatuikit.presentation.uicomponents.share

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.adapter.ShareableChannelsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.adapter.viewholders.ShareableChannelViewHolderFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class SceytShareableActivity : AppCompatActivity(), SceytKoinComponent {
    private val viewModel: ChannelsViewModel by viewModel()
    private var channelsAdapter: ShareableChannelsAdapter? = null
    private val viewHolderFactory by lazy { ShareableChannelViewHolderFactory(this) }
    protected val selectedChannels = mutableSetOf<SceytChannel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        viewModel.getChannels(0)


    }

    private fun initViewModel() {
        viewModel.loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleScope)
    }

    protected open suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        lifecycleScope.launch {
            when (response) {
                is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
                is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
                else -> return@launch
            }
        }
    }

    protected open suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        val data = viewModel.mapToChannelItem(data = response.data, hasNext = response.hasNext)
        if (response.offset == 0) {
            setChannelsList(data)
        } else {
            addNewChannels(data)
        }
    }

    protected open suspend fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newChannels = viewModel.mapToChannelItem(data = response.cacheData, hasNext = response.hasNext)
                    setChannelsList(newChannels)
                } else if (!viewModel.hasNextDb) channelsAdapter?.removeLoading()
            }
            is SceytResponse.Error -> {
                if (!viewModel.hasNextDb) channelsAdapter?.removeLoading()
            }
        }
    }

    protected open fun setChannelsList(data: List<ChannelListItem>) {
        val rv = getRV() ?: return
        if (channelsAdapter == null) {
            channelsAdapter = ShareableChannelsAdapter(data.toMutableList(), viewHolderFactory.also {
                it.setChannelClickListener { channelItem ->
                    onChannelsClick(channelItem.channel)
                }
            }).also { channelsAdapter = it }
            rv.adapter = channelsAdapter
            rv.layoutManager = LinearLayoutManager(this)
        } else {
            channelsAdapter?.notifyUpdate(data, rv)
        }
    }

    protected open fun addNewChannels(data: List<ChannelListItem>) {
        channelsAdapter?.addList(data as MutableList<ChannelListItem>)
    }

    @CallSuper
    protected open fun onChannelsClick(channel: SceytChannel) {
        if (selectedChannels.contains(channel))
            selectedChannels.remove(channel)
        else selectedChannels.add(channel)
    }

    protected fun enableNext(): Boolean {
        return selectedChannels.isNotEmpty()
    }

    override fun onBackPressed() {
        finish()
    }

    open fun getRV(): RecyclerView? {
        return null
    }
}