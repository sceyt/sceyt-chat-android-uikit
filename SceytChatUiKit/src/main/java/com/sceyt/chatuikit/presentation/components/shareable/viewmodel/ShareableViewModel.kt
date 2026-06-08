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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class ShareableViewModel(
    internal val config: ChannelListConfig = ChannelListConfig.default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private val getChannelsJobs: MutableSet<Job> = ConcurrentHashMap.newKeySet()
    private var nextOffset = 0
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
        if (offset == 0) nextOffset = 0
        searchQuery = query
        setPagingLoadingStarted(
            PaginationResponse.LoadType.LoadNext,
            ignoreDatabase = ignoreDatabase
        )

        notifyPageLoadingState(false)

        cancelGetChannelJobs()
        launchChannelsLoad(offset, query, loadKey, onlyMine, ignoreDatabase)
    }

    fun loadMoreChannels() {
        if (!canLoadNext()) return
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)
        notifyPageLoadingState(false)
        val state = state.value
        // Does not cancel the in-flight load — that would drop its page.
        launchChannelsLoad(
            offset = nextOffset,
            query = searchQuery,
            loadKey = LoadKeyData(value = state.channels.lastOrNull()?.id ?: 0),
            onlyMine = true,
            ignoreDatabase = false
        )
    }

    private fun launchChannelsLoad(
        offset: Int,
        query: String,
        loadKey: LoadKeyData?,
        onlyMine: Boolean,
        ignoreDatabase: Boolean,
    ) {
        val job = viewModelScope.launch(ioDispatcher) {
            channelInteractor.loadChannels(
                offset = offset,
                searchQuery = query,
                loadKey = loadKey,
                onlyMine = onlyMine,
                ignoreDb = ignoreDatabase,
                awaitForConnection = true,
                config = config
            ).collect(::initPaginationResponse)
        }.also { job ->
            job.invokeOnCompletion { getChannelsJobs.remove(job) }
        }
        getChannelsJobs.add(job)
    }

    private fun cancelGetChannelJobs() {
        val jobs = getChannelsJobs.toList()
        getChannelsJobs.clear()
        jobs.forEach { it.cancel() }
    }

    fun onSearchQueryChanged(query: String) {
        if (searchQuery == query) return
        searchQuery = query
        getChannels(0, query)
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                nextOffset = response.offset + response.data.size
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
                    response = SceytResponse.Success(null),
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
