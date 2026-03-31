package com.sceyt.chatuikit.presentation.components.shareable.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelListState
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ShareableViewModel(
    internal val config: ChannelListConfig = ChannelListConfig.default,
) : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private var getChannelsJob: Job? = null
    val selectedChannels = mutableSetOf<Long>()

    var searchQuery = ""
        private set

    private val _state = MutableStateFlow(ChannelListState())
    val state: StateFlow<ChannelListState> = _state

    init {
        getChannels(0)
    }

    private fun getChannels(
        offset: Int,
        query: String = searchQuery,
        loadKey: LoadKeyData? = null,
        onlyMine: Boolean = true,
        ignoreDatabase: Boolean = false,
    ) {
        searchQuery = query
        setPagingLoadingStarted(
            PaginationResponse.LoadType.LoadNext,
            ignoreDatabase = ignoreDatabase
        )

        notifyPageLoadingState(false)

        getChannelsJob?.cancel()
        getChannelsJob = viewModelScope.launch(Dispatchers.IO) {
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

    fun loadMoreChannels() {
        if (!canLoadNext()) return
        val state = state.value
        getChannels(
            offset = state.channels.size,
            query = searchQuery,
            loadKey = LoadKeyData(value = state.channels.lastOrNull()?.id ?: 0)
        )
    }

    fun onSearchQueryChanged(query: String) {
        if (searchQuery == query) return
        searchQuery = query
        getChannels(0, query)
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                _state.update { current ->
                    if (response.offset == 0)
                        current.copy(
                            channels = response.data,
                            hasNext = response.hasNext
                        )
                    else {
                        current.copy(
                            channels = current.channels + response.data,
                            hasNext = response.hasNext
                        )
                    }
                }
                notifyPageStateWithResponse(
                    SceytResponse.Success(null),
                    wasLoadingMore = response.offset > 0,
                    isEmpty = response.data.isEmpty(),
                    searchQuery = response.query
                )
            }

            else -> Unit
        }
        pagingResponseReceived(response)
    }
}
