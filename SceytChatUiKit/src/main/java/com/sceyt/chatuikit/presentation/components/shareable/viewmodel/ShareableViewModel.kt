package com.sceyt.chatuikit.presentation.components.shareable.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ShareableViewModel(
        internal val config: ChannelListConfig = ChannelListConfig.default,
) : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private var getChannelsJog: Job? = null
    val selectedChannels = mutableSetOf<Long>()

    var searchQuery = ""
        private set

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<SceytChannel>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<SceytChannel>> = _loadChannelsFlow

    fun getChannels(
            offset: Int,
            query: String = searchQuery,
            loadKey: LoadKeyData? = null,
            onlyMine: Boolean = true,
            ignoreDatabase: Boolean = false,
    ) {
        searchQuery = query
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext, ignoreDatabase = ignoreDatabase)

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.loadChannels(
                offset = offset,
                searchQuery = query,
                loadKey = loadKey,
                onlyMine = onlyMine,
                ignoreDb = ignoreDatabase,
                awaitForConnection = true,
                config = config
            ).collect(::initPaginationResponse)
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                _loadChannelsFlow.value = response
                notifyPageStateWithResponse(SceytResponse.Success(null),
                    wasLoadingMore = response.offset > 0,
                    isEmpty = response.data.isEmpty(), searchQuery = response.query)
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    internal fun mapToChannelItem(
            data: List<SceytChannel>?, hasNext: Boolean,
            includeDirectChannelsWithDeletedPeers: Boolean = true,
    ): List<ChannelListItem> {

        val filteredChannels = if (includeDirectChannelsWithDeletedPeers) data ?: emptyList()
        else data?.filter { channel -> !channel.isPeerDeleted() }
                ?: emptyList()

        if (filteredChannels.isEmpty())
            return emptyList()

        val channelItems = filteredChannels.map { ChannelItem(it) }

        return if (hasNext)
            channelItems + ChannelListItem.LoadingMoreItem
        else
            channelItems
    }
}
