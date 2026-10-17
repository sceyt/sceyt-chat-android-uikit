package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Integration tests reproducing the "missing channels" report:
 * with 1000+ channels the list shows the first page plus a few channels without a last
 * message, and the rest never load.
 *
 * These tests drive the real [ChannelsViewModel] + real [ChannelsCache] through the same
 * mocked-interactor harness used by [ChannelsViewModelPaginationTest]. They encode the
 * EXPECTED (correct) behavior, so they FAIL on the current code and will pass once the
 * bugs are fixed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelListMissingChannelsReproTest {
    private val dispatcher = StandardTestDispatcher()
    private val interactor = mock<ChannelInteractor>()
    private val channelsCache = ChannelsCache()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<ChannelInteractor> { interactor } })
        }
    }

    @After
    fun tearDown() = runBlocking {
        Dispatchers.resetMain()
        channelsCache.clearAll()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    /**
     * BUG 3 — a scroll-triggered load-more that races an in-flight first-page server response
     * drops that server response, so server-only channels go missing.
     *
     * The first load emits the DB page immediately, then suspends waiting for the server. While it
     * is suspended `hasNextDb == true` already makes `canLoadNext()` true (the deliberate DB-first
     * fast-path), so a scroll-to-end fires `loadMoreChannels`. `getChannels` then runs
     * `getChannelsJog?.cancel()`, killing the still-suspended offset-0 job — its `ServerResponse`
     * (and, in production, its `saveChannelsToDb` write + the SDK query cursor advance) is lost.
     *
     * This asserts the OUTCOME, not a specific remedy: the server-only channel (id 999) delivered
     * by the first page must survive the race. Any fix works — defer/queue the load-more, keep the
     * in-flight job alive, or make server paging stateless — they all keep 999. Only the buggy
     * cancel-and-forget drops it.
     */
    @Test
    fun `load more racing an in-flight first page must not drop the first page server data`() =
        runTest(dispatcher) {
            val config = ChannelListConfig(
                types = emptyList(),
                order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
                queryLimit = 20,
                queryParam = ChannelQueryParam(1, 10, 1, true)
            )
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            val secondPage = (21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            // A channel only the SERVER knows about on the first page — not yet in the local DB.
            val serverOnlyChannel = createChannel(999, pinnedAt = 0, createdAt = 999)
            // Server PAGE stays exactly queryLimit (20): it drops one DB row and adds the server-only
            // channel. The merged cache is still 21 (DB kept channel 20), which is the realistic state.
            val serverPage = firstPage.dropLast(1) + serverOnlyChannel
            val mergedCache = (firstPage + serverOnlyChannel)
                .sortedWith(ChannelsComparatorDescBy(config.order))
            var persistedCache = firstPage.sortedWith(ChannelsComparatorDescBy(config.order))

            val dbPageEmitted = CompletableDeferred<Unit>()
            val releaseServerPage = CompletableDeferred<Unit>()

            whenever(
                interactor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                flow {
                    when (offset) {
                        0 -> {
                            emit(
                                PaginationResponse.DBResponse(
                                    data = firstPage, loadKey = loadKey, offset = 0,
                                    hasNext = true, query = query
                                )
                            )
                            dbPageEmitted.complete(Unit)
                            releaseServerPage.await() // server still loading
                            persistedCache = mergedCache
                            emit(
                                PaginationResponse.ServerResponse(
                                    data = SceytResponse.Success(serverPage), // exactly queryLimit (20)
                                    cacheData = mergedCache,                  // merged cache (21)
                                    loadKey = loadKey, offset = 0,
                                    hasDiff = true, hasNext = true, hasPrev = false,
                                    loadType = PaginationResponse.LoadType.LoadNext,
                                    ignoredDb = false, query = query
                                )
                            )
                        }

                        20 -> {
                            emit(
                                PaginationResponse.DBResponse(
                                    data = secondPage, loadKey = loadKey, offset = 20,
                                    hasNext = false, query = query
                                )
                            )
                            emit(
                                PaginationResponse.ServerResponse(
                                    data = SceytResponse.Success(secondPage),
                                    cacheData = (persistedCache + secondPage)
                                        .distinctBy { it.id }
                                        .sortedWith(ChannelsComparatorDescBy(config.order)),
                                    loadKey = loadKey, offset = 20,
                                    hasDiff = true, hasNext = false, hasPrev = false,
                                    loadType = PaginationResponse.LoadType.LoadNext,
                                    ignoredDb = false, query = query
                                )
                            )
                        }
                    }
                }
            }

            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()
            dbPageEmitted.await()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20) // DB page only, server pending

            // Scroll to end while the first page's server response is still in flight.
            viewModel.loadMoreChannels(viewModel.state.value.channels.lastOrNull()?.id)
            advanceUntilIdle()

            // Now the first page's server response completes.
            releaseServerPage.complete(Unit)
            advanceUntilIdle()

            // The server-only channel from the first page must not have been lost to the race.
            assertThat(viewModel.state.value.channels.map { it.id }).contains(999L)
        }

    /**
     * BUG 2 — pagination must recover after a transient server error.
     *
     * Matches the 16:03:39 log line: `getChannels error: Not connected, code: 9903`.
     *
     * Fixed in persistence: `loadChannels` now emits a terminal `ServerResponse` carrying the error
     * even on failure. This test guards the ViewModel side of that contract: given the failure
     * arrives as a `ServerResponse`, `onPaginationSeverResponse` clears `loadingNextItems`, so a
     * later scroll retries. Offset 20 errors once, then the retry reaches 40 channels. (Before the
     * fix, NO `ServerResponse` was emitted on error, so `loadingNextItems` stayed stuck `true` and
     * `canLoadNext()` was false forever.)
     */
    @Test
    fun `transient server error on load more must not permanently freeze pagination`() =
        runTest(dispatcher) {
            val config = ChannelListConfig(
                types = emptyList(),
                order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
                queryLimit = 20,
                queryParam = ChannelQueryParam(1, 10, 1, true)
            )
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            val secondPage = (21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) }

            // attempts-per-offset, so offset 20 can fail once then succeed on retry
            val attempts = mutableMapOf<Int, Int>()
            whenever(
                interactor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                val attempt = (attempts[offset] ?: 0) + 1
                attempts[offset] = attempt
                flow {
                    when (offset) {
                        0 -> {
                            // DB holds only the first page; server still has more.
                            emit(
                                PaginationResponse.DBResponse(
                                    data = firstPage, loadKey = loadKey, offset = 0,
                                    hasNext = false, query = query
                                )
                            )
                            emit(
                                PaginationResponse.ServerResponse(
                                    data = SceytResponse.Success(firstPage),
                                    cacheData = firstPage, loadKey = loadKey, offset = 0,
                                    hasDiff = false, hasNext = true, hasPrev = false,
                                    loadType = PaginationResponse.LoadType.LoadNext,
                                    ignoredDb = false, query = query
                                )
                            )
                        }

                        20 -> {
                            emit(
                                PaginationResponse.DBResponse(
                                    data = emptyList(), loadKey = loadKey, offset = 20,
                                    hasNext = false, query = query
                                )
                            )
                            if (attempt == 1) {
                                // Server errored — loadChannels still emits a terminal ServerResponse
                                // carrying the error (the persistence fix).
                                emit(
                                    PaginationResponse.ServerResponse(
                                        data = SceytResponse.Error(SceytException(9903, "Not connected")),
                                        cacheData = firstPage, loadKey = loadKey, offset = 20,
                                        hasDiff = false, hasNext = false, hasPrev = false,
                                        loadType = PaginationResponse.LoadType.LoadNext,
                                        ignoredDb = false, query = query
                                    )
                                )
                            } else {
                                emit(
                                    PaginationResponse.ServerResponse(
                                        data = SceytResponse.Success(secondPage),
                                        cacheData = firstPage + secondPage,
                                        loadKey = loadKey, offset = 20,
                                        hasDiff = true, hasNext = false, hasPrev = false,
                                        loadType = PaginationResponse.LoadType.LoadNext,
                                        ignoredDb = false, query = query
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20)
            assertThat(viewModel.canLoadNext()).isTrue()

            // First load-more: server errors, nothing new arrives.
            viewModel.loadMoreChannels(viewModel.state.value.channels.lastOrNull()?.id)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20)

            // The error was transient — the user scrolls again and the page should now load.
            // This is exactly what the buggy code blocks: loadingNextItems is stuck true.
            assertThat(viewModel.canLoadNext()).isTrue()

            viewModel.loadMoreChannels(viewModel.state.value.channels.lastOrNull()?.id)
            advanceUntilIdle()

            assertThat(attempts[20]).isEqualTo(2) // a retry was actually issued
            assertThat(viewModel.state.value.channels.size).isEqualTo(40)
        }

    /**
     * Job-set refactor: a refresh (getChannels) must cancel an in-flight load-more via
     * cancelGetChannelJobs, so a page that resolves AFTER the refresh is not applied on top of the
     * fresh first page. (load-more itself never cancels — see the race test above.)
     */
    @Test
    fun `refresh cancels an in-flight load more so its stale page is not applied`() =
        runTest(dispatcher) {
            val config = ChannelListConfig(
                types = emptyList(),
                order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
                queryLimit = 20,
                queryParam = ChannelQueryParam(1, 10, 1, true)
            )
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            val secondPage = (21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            val releaseLoadMore = CompletableDeferred<Unit>()

            whenever(
                interactor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                flow {
                    when (offset) {
                        0 -> {
                            emit(
                                PaginationResponse.DBResponse(
                                    data = firstPage, loadKey = loadKey, offset = 0,
                                    hasNext = true, query = query
                                )
                            )
                            emit(
                                PaginationResponse.ServerResponse(
                                    data = SceytResponse.Success(firstPage),
                                    cacheData = firstPage, loadKey = loadKey, offset = 0,
                                    hasDiff = false, hasNext = true, hasPrev = false,
                                    loadType = PaginationResponse.LoadType.LoadNext,
                                    ignoredDb = false, query = query
                                )
                            )
                        }

                        20 -> {
                            releaseLoadMore.await() // load-more stays in flight until released
                            emit(
                                PaginationResponse.ServerResponse(
                                    data = SceytResponse.Success(secondPage),
                                    cacheData = firstPage + secondPage,
                                    loadKey = loadKey, offset = 20,
                                    hasDiff = true, hasNext = false, hasPrev = false,
                                    loadType = PaginationResponse.LoadType.LoadNext,
                                    ignoredDb = false, query = query
                                )
                            )
                        }
                    }
                }
            }

            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20)

            // Start a load-more that won't resolve yet.
            viewModel.loadMoreChannels(viewModel.state.value.channels.lastOrNull()?.id)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20)

            // Refresh must cancel the in-flight load-more.
            viewModel.getChannels(query = "")
            advanceUntilIdle()

            // The stale load-more now resolves; its page must NOT be applied.
            releaseLoadMore.complete(Unit)
            advanceUntilIdle()

            assertThat(viewModel.state.value.channels.size).isEqualTo(20)
            assertThat(viewModel.state.value.channels.map { it.id })
                .containsNoneIn(secondPage.map { it.id })
        }
}
