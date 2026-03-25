package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsItemComparatorBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ChannelsViewModel(
    internal val config: ChannelListConfig = ChannelListConfig.default,
) : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private var getChannelsJog: Job? = null
    private var sortJob: Job? = null

    var searchQuery = ""
        private set

    private val _channelItems = MutableStateFlow<List<ChannelListItem>>(emptyList())
    val channelItems: StateFlow<List<ChannelListItem>> = _channelItems

    init {
        // Initial load — triggers server sync and populates from DB
        getChannels(0, query = searchQuery)

        ChannelsCache.channelsDeletedFlow.onEach { ids ->
            _channelItems.update { items ->
                val filtered = items.filter { (it as? ChannelItem)?.channel?.id !in ids }
                if (filtered.none { it is ChannelItem })
                    pageStateLiveDataInternal.postValue(PageState.StateEmpty(searchQuery))
                filtered
            }
        }.launchIn(viewModelScope)

        ChannelsCache.channelUpdatedFlow
            .filter { config.isValidForConfig(it.channel) }
            .onEach { data ->
                _channelItems.update { items ->
                    var found = false
                    val updated = items.map {
                        if (it is ChannelItem && it.channel.id == data.channel.id) {
                            found = true; it.copy(channel = data.channel)
                        } else it
                    }
                    if (found) updated else updated + ChannelItem(data.channel)
                }
                if (data.diff.lastMessageChanged || data.needSorting)
                    sortItemsDebounced()
            }.launchIn(viewModelScope)

        ChannelsCache.channelReactionMsgLoadedFlow.onEach { channel ->
            _channelItems.update { items ->
                items.map {
                    if (it is ChannelItem && it.channel.id == channel.id)
                        it.copy(channel = channel) else it
                }
            }
        }.launchIn(viewModelScope)

        ChannelsCache.channelAddedFlow
            .filter { config.isValidForConfig(it) }
            .onEach { channel ->
                _channelItems.update { items ->
                    if (items.none { (it as? ChannelItem)?.channel?.id == channel.id }) {
                        (items + ChannelItem(channel))
                            .filterIsInstance<ChannelItem>()
                            .sortedWith(ChannelsItemComparatorBy(config.order))
                            .withLoadingMore()
                    } else items
                }
            }.launchIn(viewModelScope)

        ChannelsCache.pendingChannelCreatedFlow
            .filter { (_, channel) -> config.isValidForConfig(channel) }
            .onEach { (pendingId, newChannel) ->
                _channelItems.update { items ->
                    val without = items.filter { (it as? ChannelItem)?.channel?.id != pendingId }
                    if (without.none { (it as? ChannelItem)?.channel?.id == newChannel.id })
                        without + ChannelItem(newChannel)
                    else without
                }
            }.launchIn(viewModelScope)

        ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
            _channelItems.update { items ->
                items.map {
                    if (it is ChannelItem && it.channel.id == channel.id)
                        it.copy(channel = channel) else it
                }
            }
        }.launchIn(viewModelScope)

        ChannelsCache.newChannelsOnSync.onEach { (_, channels) ->
            _channelItems.update { items ->
                val existing = items.mapNotNull { (it as? ChannelItem)?.channel }
                val newItems =
                    initDataOnNewChannelsOnSync(existing, channels) ?: return@update items
                newItems
            }
        }.launchIn(viewModelScope)
    }

    // Appends or removes LoadingMoreItem based on current pagination state
    private fun List<ChannelListItem>.withLoadingMore(): List<ChannelListItem> {
        val base = filter { it !is ChannelListItem.LoadingMoreItem }
        return if (hasNext || hasNextDb) base + ChannelListItem.LoadingMoreItem else base
    }

    // Debounced sort — cancels the previous sort if a newer update arrives within 300 ms
    private fun sortItemsDebounced() {
        sortJob?.cancel()
        sortJob = viewModelScope.launch(Dispatchers.Default) {
            delay(300)
            _channelItems.update { items ->
                items.filterIsInstance<ChannelItem>()
                    .sortedWith(ChannelsItemComparatorBy(config.order))
                    .withLoadingMore()
            }
        }
    }

    fun getChannels(
        offset: Int,
        query: String = searchQuery,
        loadKey: LoadKeyData? = null,
        onlyMine: Boolean = query.isEmpty(),
        ignoreDatabase: Boolean = false,
    ) {
        searchQuery = query
        setPagingLoadingStarted(
            loadType = PaginationResponse.LoadType.LoadNext,
            ignoreDatabase = ignoreDatabase
        )

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

    @Suppress("unused")
    fun searchChannelsWithUserIds(
        offset: Int,
        query: String = searchQuery,
        userIds: List<String> = emptyList(),
        directChatType: String = ChannelTypeEnum.Direct.value,
        config: ChannelListConfig = this.config,
        onlyMine: Boolean = false,
        includeSearchByUserDisplayName: Boolean = false,
        ignoreDatabase: Boolean = false,
        loadKey: LoadKeyData? = null,
    ) {
        searchQuery = query
        setPagingLoadingStarted(
            loadType = PaginationResponse.LoadType.LoadNext,
            ignoreDatabase = ignoreDatabase
        )

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.searchChannelsWithUserIds(
                offset = offset,
                searchQuery = query,
                userIds = userIds,
                config = config,
                includeSearchByUserDisplayName = includeSearchByUserDisplayName,
                onlyMine = onlyMine,
                ignoreDb = ignoreDatabase,
                loadKey = loadKey,
                directChatType = directChatType
            ).collect(::initPaginationResponse)
        }
    }

    @Suppress("unused")
    fun searchLocalChannelsBySQLiteQuery(
        searchQuery: String,
        sqLiteQuery: SimpleSQLiteQuery,
    ) {
        this.searchQuery = searchQuery
        setPagingLoadingStarted(
            loadType = PaginationResponse.LoadType.LoadNext,
            ignoreDatabase = false,
            ignoreServer = true
        )

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.getChannelsBySQLiteQuery(sqLiteQuery)
            val paginationResponse = PaginationResponse.DBResponse(
                data = response,
                offset = 0,
                query = searchQuery,
                loadKey = null
            )
            initPaginationResponse(paginationResponse)
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    val items = mapToChannelItem(response.data, response.hasNext)
                    _channelItems.update { current ->
                        if (response.offset == 0) items
                        else current.filter { it !is ChannelListItem.LoadingMoreItem } + items
                    }
                    notifyPageStateWithResponse(
                        response = SceytResponse.Success(null),
                        wasLoadingMore = response.offset > 0,
                        isEmpty = response.data.isEmpty(), searchQuery = response.query
                    )
                }
            }

            is PaginationResponse.ServerResponse -> {
                if (response.data is SceytResponse.Success && response.hasDiff) {
                    val items = mapToChannelItem(response.cacheData, response.hasNext)
                    _channelItems.update { items }
                } else if (!hasNextDb) {
                    _channelItems.update { it.withLoadingMore() }
                }
                notifyPageStateWithResponse(
                    response = response.data,
                    wasLoadingMore = response.offset > 0,
                    isEmpty = response.cacheData.isEmpty(),
                    searchQuery = response.query
                )
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    internal suspend fun initDataOnNewChannelsOnSync(
        existingChannels: List<SceytChannel>,
        syncChannels: List<SceytChannel>,
    ): List<ChannelListItem>? = withContext(Dispatchers.Default) {
        // Filter channels by config
        val filtered = syncChannels.filter { config.isValidForConfig(it) }
        if (filtered.isEmpty()) return@withContext null

        val existing = existingChannels.toMutableSet()
        // If loadedChannels are empty and not loading data from server, it means we can setData,
        // otherwise we filter only channels which are between loaded channels and
        // insert them to the list.
        if (existing.isEmpty()) {
            if (loadingFromServer || loadingFromDb) return@withContext null
            val sorted = filtered.sortedWith(ChannelsComparatorDescBy(config.order))
            val date = mapToChannelItem(data = sorted, hasNext = false)
            SceytLog.i(
                "syncResultUpdate",
                "loaded channels are empty, set data : ${sorted.map { it.id }}"
            )
            return@withContext date
        } else {
            // Get last channel to understand where to insert new channels
            val lastChannel = existing.last()
            val sorted = filtered.toSet().plus(lastChannel)
                .sortedWith(ChannelsComparatorDescBy(config.order))
            val index = sorted.indexOf(lastChannel)

            // If index is last, and we have more channels, we don't need to insert them,
            // because they will be inserted by next page loading
            if (index == existing.size - 1 && (hasNext || hasNextDb)) {
                return@withContext null
            }
            // Get channels which need to be inserted
            sorted.subList(0, index).forEach {
                existing.add(it)
            }
            var newData: List<ChannelListItem> =
                existing.sortedWith(ChannelsComparatorDescBy(config.order)).map(::ChannelItem)

            if (hasNext || hasNextDb)
                newData = newData.plus(ChannelListItem.LoadingMoreItem)

            SceytLog.i(
                "syncResultUpdate", "should be applied synced channels : ${
                    newData.map { (it as? ChannelItem)?.channel?.id ?: it.toString() }
                }")
            return@withContext newData
        }
    }

    internal fun mapToChannelItem(
        data: List<SceytChannel>?, hasNext: Boolean,
        includeDirectChannelsWithDeletedPeers: Boolean = true,
    ): List<ChannelListItem> {

        val filteredChannels = if (includeDirectChannelsWithDeletedPeers) data.orEmpty()
        else data?.filter { channel -> !channel.isPeerDeleted() }.orEmpty()

        if (filteredChannels.isEmpty())
            return emptyList()

        val channelItems = filteredChannels.map { ChannelItem(it) }

        return if (hasNext)
            channelItems + ChannelListItem.LoadingMoreItem
        else
            channelItems
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun markChannelAsUnRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsUnRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.clearHistory(channelId, forEveryone)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.deleteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.leaveChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.muteChannel(channelId, muteUntil)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unMuteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun pinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.pinChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun unpinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unpinChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    @Suppress("unused")
    fun hideChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.hideChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    @Suppress("unused")
    fun unHideChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unHideChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    internal fun onChannelCommandEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> markChannelAsRead(event.channel.id)
            is ChannelEvent.MarkAsUnRead -> markChannelAsUnRead(event.channel.id)
            is ChannelEvent.ClearHistory -> clearHistory(event.channel.id, event.channel.isPublic())
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel.id)
            is ChannelEvent.DeleteChannel -> deleteChannel(event.channel.id)
            is ChannelEvent.Mute -> muteChannel(event.channel.id, event.muteUntil)
            is ChannelEvent.UnMute -> unMuteChannel(event.channel.id)
            is ChannelEvent.Pin -> pinChannel(event.channel.id)
            is ChannelEvent.UnPin -> unpinChannel(event.channel.id)
        }
    }
}
