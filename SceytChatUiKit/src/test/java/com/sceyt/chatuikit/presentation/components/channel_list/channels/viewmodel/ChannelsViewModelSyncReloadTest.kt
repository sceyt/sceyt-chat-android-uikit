package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SyncedChannelsWindow
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests the post-sync UI refresh: when a channel sync finishes, the ViewModel rebuilds its loaded
 * window from the DB (via [ChannelInteractor.reloadChannelsAfterSync]) instead of merging deltas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsViewModelSyncReloadTest {
    private val dispatcher = StandardTestDispatcher()
    private val interactor = mock<ChannelInteractor>()

    private val config = ChannelListConfig(
        types = emptyList(),
        order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
        queryLimit = 20,
        queryParam = ChannelQueryParam(1, 10, 1, true)
    )

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
    fun tearDown() {
        Dispatchers.resetMain()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    // Stubs the initial getChannels(offset 0) load with a single page so the VM settles with a window.
    private suspend fun stubInitialPage(page: List<SceytChannel>) {
        val sorted = page.sortedWith(ChannelsComparatorDescBy(config.order))
        whenever(
            interactor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
        ).thenAnswer { invocation ->
            val offset = invocation.getArgument<Int>(0)
            val query = invocation.getArgument<String>(1)
            val loadKey = invocation.getArgument<LoadKeyData?>(2)
            flow {
                if (offset == 0) {
                    emit(PaginationResponse.DBResponse(data = page, loadKey = loadKey, offset = 0, hasNext = true, query = query))
                    emit(
                        PaginationResponse.ServerResponse(
                            data = SceytResponse.Success(page), cacheData = sorted, loadKey = loadKey,
                            offset = 0, hasDiff = true, hasNext = true, hasPrev = false,
                            loadType = PaginationResponse.LoadType.LoadNext, ignoredDb = false, query = query
                        )
                    )
                }
            }
        }
    }

    private fun emitSyncFinished() {
        SceytSyncManager.syncChannelsResult_.tryEmit(SyncResult.SuccessfullyFinished)
    }

    private fun emitProportion(items: List<SceytChannel>) {
        SceytSyncManager.syncChannelsResult_.tryEmit(SyncResult.Proportion(items))
    }

    private fun nextOffset(viewModel: ChannelsViewModel): Int {
        val field = ChannelsViewModel::class.java.getDeclaredField("nextOffset")
        field.isAccessible = true
        return field.getInt(viewModel)
    }

    @Test
    fun `sync finish rebuilds the loaded window from the db and surfaces a bumped channel`() =
        runTest(dispatcher) {
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            stubInitialPage(firstPage)

            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.map { it.id }.first()).isEqualTo(20L)
            assertThat(viewModel.state.value.channels.size).isEqualTo(20)

            // Sync rebuilds the top-20 window: a previously-unloaded channel (999) was bumped to the top
            // by a new message, pushing channel 1 out of the window.
            val bumped = createChannel(999, pinnedAt = 0, createdAt = 999)
            val rebuilt = (listOf(bumped) + firstPage.drop(1))
                .sortedWith(ChannelsComparatorDescBy(config.order))
            whenever(interactor.reloadChannelsAfterSync(any(), any()))
                .thenReturn(SyncedChannelsWindow(channels = rebuilt, hasNext = true, loadedCount = 20))

            emitSyncFinished()
            advanceUntilIdle()

            val ids = viewModel.state.value.channels.map { it.id }
            assertThat(ids.first()).isEqualTo(999L)        // bumped channel surfaced at top
            assertThat(ids).doesNotContain(1L)             // pushed out of the window
            assertThat(ids.size).isEqualTo(20)
            assertThat(nextOffset(viewModel)).isEqualTo(20) // paging realigned to the rebuilt window
            assertThat(viewModel.canLoadNext()).isTrue()
        }

    @Test
    fun `sync finish is ignored while searching`() = runTest(dispatcher) {
        val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
        stubInitialPage(firstPage)

        val viewModel = ChannelsViewModel(config, dispatcher)
        advanceUntilIdle()

        // Enter search mode — its results come from globalSearchDao, not a top-N channel query.
        viewModel.getChannels(query = "abc")
        advanceUntilIdle()

        emitSyncFinished()
        advanceUntilIdle()

        verify(interactor, never()).reloadChannelsAfterSync(any(), any())
    }

    @Test
    fun `sync finishing while server is pending defers reload, then load more materializes it before paging`() =
        runTest(dispatcher) {
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            val firstPageSorted = firstPage.sortedWith(ChannelsComparatorDescBy(config.order))
            val releaseServer = CompletableDeferred<Unit>()
            val secondPageOffset = AtomicInteger(-1)

            whenever(
                interactor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                flow {
                    if (offset == 0) {
                        // DB page lands; server stays pending so loadingFromServer == true.
                        emit(PaginationResponse.DBResponse(data = firstPage, loadKey = loadKey, offset = 0, hasNext = true, query = query))
                        releaseServer.await()
                        emit(
                            PaginationResponse.ServerResponse(
                                data = SceytResponse.Success(firstPage), cacheData = firstPageSorted, loadKey = loadKey,
                                offset = 0, hasDiff = true, hasNext = true, hasPrev = false,
                                loadType = PaginationResponse.LoadType.LoadNext, ignoredDb = false, query = query
                            )
                        )
                    } else {
                        // Record the offset load-more pages from; it must be the reload-corrected value.
                        secondPageOffset.set(offset)
                    }
                }
            }

            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(20) // DB page shown, server pending

            // Reload rebuilds a wider window (loadedCount 25) with a bumped channel at the top.
            val bumped = createChannel(999, pinnedAt = 0, createdAt = 999)
            val rebuilt = (listOf(bumped) + (1L..24L).map { createChannel(it, pinnedAt = 0, createdAt = it) })
                .sortedWith(ChannelsComparatorDescBy(config.order))
            whenever(interactor.reloadChannelsAfterSync(any(), any()))
                .thenReturn(SyncedChannelsWindow(channels = rebuilt, hasNext = true, loadedCount = 25))

            // Sync finishes WHILE the server page is still pending → reload is deferred, not run.
            emitSyncFinished()
            advanceUntilIdle()
            verify(interactor, never()).reloadChannelsAfterSync(any(), any())
            assertThat(viewModel.state.value.channels.map { it.id }).doesNotContain(999L)

            // User scrolls (DB fast-path allows it while server is pending). load-more must materialize
            // the deferred reload BEFORE paging, then page from the reload-corrected offset.
            viewModel.loadMoreChannels(viewModel.state.value.channels.last().id)
            advanceUntilIdle()

            verify(interactor, times(1)).reloadChannelsAfterSync(any(), any())
            assertThat(viewModel.state.value.channels.map { it.id }).contains(999L) // reload applied
            assertThat(secondPageOffset.get()).isEqualTo(25) // paged from reload-corrected offset, not stale 20

            releaseServer.complete(Unit) // cleanup the parked first-page server coroutine
            advanceUntilIdle()
        }

    @Test
    fun `proportion covering the loaded window rebuilds and surfaces a bumped channel`() =
        runTest(dispatcher) {
            val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
            stubInitialPage(firstPage)
            val viewModel = ChannelsViewModel(config, dispatcher)
            advanceUntilIdle()

            val bumped = createChannel(999, pinnedAt = 0, createdAt = 999)
            val rebuilt = (listOf(bumped) + firstPage.drop(1))
                .sortedWith(ChannelsComparatorDescBy(config.order))
            whenever(interactor.reloadChannelsAfterSync(any(), any()))
                .thenReturn(SyncedChannelsWindow(channels = rebuilt, hasNext = true, loadedCount = 20))

            // First proportion (server's top page) carries the bumped channel — surfaces before finish.
            emitProportion(listOf(bumped) + (2L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) })
            advanceUntilIdle()

            verify(interactor, times(1)).reloadChannelsAfterSync(any(), any())
            assertThat(viewModel.state.value.channels.map { it.id }.first()).isEqualTo(999L)
        }

    @Test
    fun `proportions past the loaded window do not rebuild`() = runTest(dispatcher) {
        val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
        stubInitialPage(firstPage)
        val viewModel = ChannelsViewModel(config, dispatcher)
        advanceUntilIdle()

        whenever(interactor.reloadChannelsAfterSync(any(), any())).thenReturn(
            SyncedChannelsWindow(
                channels = firstPage.sortedWith(ChannelsComparatorDescBy(config.order)),
                hasNext = true, loadedCount = 20
            )
        )

        // Proportion 1 covers the 20-channel window → one rebuild; proportion 2 is past it → skipped.
        emitProportion((1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) })
        advanceUntilIdle()
        emitProportion((21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) })
        advanceUntilIdle()

        verify(interactor, times(1)).reloadChannelsAfterSync(any(), any())
        // The below-window proportion didn't grow the list; it stays the rebuilt 20-channel window.
        assertThat(viewModel.state.value.channels.size).isEqualTo(20)
    }
}
