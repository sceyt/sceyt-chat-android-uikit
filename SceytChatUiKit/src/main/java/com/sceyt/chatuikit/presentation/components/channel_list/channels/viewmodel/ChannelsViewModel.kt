package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.onSuccess
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.CoroutineDispatcher
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
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds both the raw channel list (for business logic) and the pre-computed adapter list.
 * [channelItems] is derived from [channels] and [hasNext]; copy() recomputes it automatically.
 * equals() compares [channelItems] via [ChannelListItem.equals] so StateFlow emits on content changes.
 */
data class ChannelListState(
    val channels: List<SceytChannel> = emptyList(),
    val hasNext: Boolean = false,
) {
    val channelItems: List<ChannelListItem> = channels.map(::ChannelItem) +
            if (hasNext) listOf(ChannelListItem.LoadingMoreItem) else emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChannelListState) return false
        return hasNext == other.hasNext && channelItems == other.channelItems
    }

    override fun hashCode(): Int = channelItems.hashCode()
}

class ChannelsViewModel(
    internal val config: ChannelListConfig = ChannelListConfig.default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private val systemMessageSender: SystemMessageSender by inject()
    private val getChannelsJobs: MutableSet<Job> = ConcurrentHashMap.newKeySet()
    private var searchChannelJob: Job? = null
    private var sortJob: Job? = null
    private var reloadJob: Job? = null

    // Set when sync finishes during paging; consumed when paging settles or before the next load-more.
    @Volatile
    private var pendingSyncReload = false
    // Channels synced so far in the current sync; used to rebuild only while the synced prefix still
    // overlaps the loaded window. Reset on sync finish/error.
    private var syncedChannelsCount = 0
    private var nextOffset = 0

    var searchQuery = ""
        private set

    private val _state = MutableStateFlow(ChannelListState())
    val state: StateFlow<ChannelListState> = _state

    private companion object {
        const val TAG = "ChannelsViewModel"
    }

    init {
        getChannels(query = searchQuery)

        ChannelsCache.channelsDeletedFlow.onEach { ids ->
            _state.update { current ->
                val filtered = current.channels.filter { it.id !in ids }
                if (filtered.isEmpty())
                    pageStateLiveDataInternal.postValue(PageState.StateEmpty(searchQuery))
                current.copy(channels = filtered)
            }
        }.launchIn(viewModelScope)

        ChannelsCache.channelUpdatedFlow
            .filter { config.isValidForConfig(it.channel) }
            .onEach { data ->
                _state.update { current ->
                    val updated = if (current.channels.any { it.id == data.channel.id })
                        current.channels.map { channel ->
                            if (channel.id == data.channel.id)
                                data.channel else channel
                        }
                    else current.channels + data.channel
                    current.copy(channels = updated)
                }
                if (data.diff.lastMessageChanged || data.needSorting)
                    sortItemsDebounced()
            }.launchIn(viewModelScope)

        ChannelsCache.channelReactionMsgLoadedFlow.onEach { channel ->
            _state.update { current ->
                current.copy(channels = current.channels.map { if (it.id == channel.id) channel else it })
            }
        }.launchIn(viewModelScope)

        ChannelsCache.channelAddedFlow
            .filter { config.isValidForConfig(it) }
            .onEach { channel ->
                _state.update { current ->
                    if (current.channels.none { it.id == channel.id })
                        current.copy(
                            channels = (current.channels + channel)
                                .sortedWith(ChannelsComparatorDescBy(config.order))
                        )
                    else current
                }
            }.launchIn(viewModelScope)

        ChannelsCache.pendingChannelCreatedFlow
            .filter { (_, channel) -> config.isValidForConfig(channel) }
            .onEach { (pendingId, newChannel) ->
                _state.update { current ->
                    val without = current.channels.filter { it.id != pendingId }
                    val merged = if (without.none { it.id == newChannel.id })
                        without + newChannel else without
                    current.copy(channels = merged)
                }
            }.launchIn(viewModelScope)

        ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
            _state.update { current ->
                current.copy(channels = current.channels.map { if (it.id == channel.id) channel else it })
            }
        }.launchIn(viewModelScope)

        // Sync can reorder the loaded window. Reload while proportions overlap the visible DB window;
        // final sync always reloads to realign paging after deletions.
        SceytSyncManager.syncChannelsResult.onEach { result ->
            when (result) {
                is SyncResult.Proportion -> {
                    val syncedBefore = syncedChannelsCount
                    syncedChannelsCount += result.items.size
                    if (syncedBefore < loadedWindowSize()) reloadAfterSync()
                }

                SyncResult.SuccessfullyFinished -> {
                    syncedChannelsCount = 0
                    reloadAfterSync()
                }

                is SyncResult.Error -> syncedChannelsCount = 0
            }
        }.launchIn(viewModelScope)
    }

    // Number of DB-backed channels currently loaded (the window the rebuild must keep aligned).
    private fun loadedWindowSize() = max(nextOffset, _state.value.channels.count { !it.pending })

    private fun sortItemsDebounced() {
        sortJob?.cancel()
        sortJob = viewModelScope.launch(Dispatchers.Default) {
            delay(200.milliseconds)
            _state.update { current ->
                current.copy(channels = current.channels.sortedWith(ChannelsComparatorDescBy(config.order)))
            }
        }
    }

    fun getChannels(
        query: String = searchQuery,
        loadKey: LoadKeyData? = null,
        onlyMine: Boolean = query.isEmpty(),
    ) {
        nextOffset = 0
        searchQuery = query
        setPagingLoadingStarted(loadType = PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(false)

        searchChannelJob?.cancel()
        cancelGetChannelJobs()
        // A full refresh owns the window, so drop any pending sync reload.
        cancelReload()

        val job = viewModelScope.launch(ioDispatcher) {
            channelInteractor.loadChannels(
                offset = 0,
                searchQuery = query,
                loadKey = loadKey,
                onlyMine = onlyMine,
                ignoreDb = false,
                awaitForConnection = true,
                config = config
            ).collect(::initPaginationResponse)
        }.also { job ->
            job.invokeOnCompletion {
                getChannelsJobs.remove(job)
            }
        }
        getChannelsJobs.add(job)
    }

    fun loadMoreChannels(lastChannelId: Long?) {
        if (!canLoadNext()) return
        setPagingLoadingStarted(loadType = PaginationResponse.LoadType.LoadNext)
        notifyPageLoadingState(true)

        val job = viewModelScope.launch(ioDispatcher) {
            if (!prepareLoadMoreAfterSync()) return@launch
            channelInteractor.loadChannels(
                offset = nextOffset,
                searchQuery = searchQuery,
                loadKey = LoadKeyData(value = lastChannelId ?: 0),
                onlyMine = searchQuery.isEmpty(),
                ignoreDb = false,
                awaitForConnection = true,
                config = config
            ).collect(::initPaginationResponse)
        }.also { job ->
            job.invokeOnCompletion {
                getChannelsJobs.remove(job)
            }
        }
        getChannelsJobs.add(job)
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

        cancelGetChannelJobs()
        searchChannelJob?.cancel()
        searchChannelJob = viewModelScope.launch(ioDispatcher) {
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
        cancelGetChannelJobs()
        searchChannelJob?.cancel()

        searchChannelJob = viewModelScope.launch(ioDispatcher) {
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
                nextOffset = response.offset + response.data.size
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    val channels = mapToChannels(response.data)
                    _state.update { current ->
                        if (response.offset == 0)
                            current.copy(channels = channels, hasNext = response.hasNext)
                        else
                            current.copy(
                                channels = (current.channels + channels).distinct(),
                                hasNext = response.hasNext
                            )
                    }
                    notifyPageStateWithResponse(
                        response = SceytResponse.Success(null),
                        wasLoadingMore = response.offset > 0,
                        isEmpty = response.data.isEmpty(), searchQuery = response.query
                    )
                }
            }

            is PaginationResponse.ServerResponse -> {
                response.data.onSuccess { channels ->
                    val pageSize = channels?.size ?: 0
                    nextOffset = max(nextOffset, response.offset + pageSize)
                    if (response.hasDiff) {
                        _state.update { state ->
                            state.copy(
                                channels = mapToChannels(response.cacheData),
                                hasNext = response.hasNext
                            )
                        }
                    } else if (!hasNextDb) {
                        _state.update { state ->
                            state.copy(hasNext = response.hasNext)
                        }
                    }
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
        if (pendingSyncReload && !loadingFromServer && !loadingFromDb)
            reloadAfterSync()
    }

    /**
     * Rebuilds the visible DB-backed window after sync. If paging is active, the reload is deferred
     * until paging settles or before the next load-more.
     */
    @VisibleForTesting
    internal fun reloadAfterSync() {
        // Search results come from globalSearchDao, not the channel table; never overwrite them.
        if (searchQuery.isNotEmpty()) return
        if (loadingFromServer || loadingFromDb) {
            pendingSyncReload = true
            SceytLog.i(
                TAG,
                "sync result while paging; reload deferred (loadingFromServer=$loadingFromServer, loadingFromDb=$loadingFromDb)"
            )
            return
        }
        pendingSyncReload = false
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch(ioDispatcher) {
            performSyncReload()
        }
    }

    private suspend fun prepareLoadMoreAfterSync(): Boolean {
        reloadJob?.join()
        if (pendingSyncReload) {
            pendingSyncReload = false
            performSyncReload()
        }

        if (hasNextDb || hasNext) return true

        finishLoadingMoreWithoutLoad()
        return false
    }

    private suspend fun performSyncReload() {
        if (searchQuery.isNotEmpty()) return
        val current = _state.value.channels
        val window = channelInteractor.reloadChannelsAfterSync(config, loadedWindowSize())
        if (current.isEmpty() && window.channels.isEmpty()) return
        nextOffset = max(nextOffset, window.loadedCount)
        hasNextDb = window.hasNext
        hasNext = window.hasNext
        _state.update {
            it.copy(
                channels = mapToChannels(window.channels),
                hasNext = window.hasNext
            )
        }
        if (window.channels.isEmpty())
            pageStateLiveDataInternal.postValue(PageState.StateEmpty(searchQuery))
        SceytLog.i(
            TAG,
            "applied sync reload: shown=${window.channels.size}, loadedCount=${window.loadedCount}, " +
                    "hasNext=${window.hasNext}, hasNextDb=$hasNextDb, nextOffset=$nextOffset"
        )
    }

    // Clears the load-more flags + footer without issuing a page (the reload already exhausted the window).
    private fun finishLoadingMoreWithoutLoad() {
        loadingNextItemsDb.set(false)
        loadingNextItems.set(false)
        notifyPageStateWithResponse(
            response = SceytResponse.Success(null),
            wasLoadingMore = true,
            searchQuery = searchQuery
        )
    }

    private fun cancelReload() {
        pendingSyncReload = false
        reloadJob?.cancel()
    }

    internal fun mapToChannels(
        data: List<SceytChannel>?,
        includeDirectChannelsWithDeletedPeers: Boolean = true,
    ): List<SceytChannel> {
        return if (includeDirectChannelsWithDeletedPeers) data.orEmpty()
        else data?.filter { channel -> !channel.isPeerDeleted() }.orEmpty()
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

    fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            if (channel.isGroup) {
                runCatching {
                    systemMessageSender.sendMemberLeft(channel.id)
                }.onFailure {
                    SceytLog.e(TAG, "Failed to send member-left system message: ${it.message}")
                }
            }

            val response = channelInteractor.leaveChannel(channel.id)
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
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel)
            is ChannelEvent.DeleteChannel -> deleteChannel(event.channel.id)
            is ChannelEvent.Mute -> muteChannel(event.channel.id, event.muteUntil)
            is ChannelEvent.UnMute -> unMuteChannel(event.channel.id)
            is ChannelEvent.Pin -> pinChannel(event.channel.id)
            is ChannelEvent.UnPin -> unpinChannel(event.channel.id)
        }
    }

    private fun cancelGetChannelJobs() {
        val jobs = getChannelsJobs.toList()
        getChannelsJobs.clear()
        jobs.forEach { it.cancel() }
    }
}
