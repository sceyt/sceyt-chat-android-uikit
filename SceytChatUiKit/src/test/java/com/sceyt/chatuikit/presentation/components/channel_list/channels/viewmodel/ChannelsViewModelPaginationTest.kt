package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsViewModelPaginationTest {
    private val dispatcher = StandardTestDispatcher()
    private val requests = mutableListOf<LoadRequest>()
    private val completedRequests = mutableListOf<CompletedRequest>()
    private val interactor = mock<ChannelInteractor>()
    private val channelsCache = ChannelsCache()
    private val config = ChannelListConfig(
        types = emptyList(),
        order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
        queryLimit = 20,
        queryParam = ChannelQueryParam(1, 10, 1, true)
    )
    private val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
    private val secondPage = (21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
    private val thirdPage = (41L..60L).map { createChannel(it, pinnedAt = 0, createdAt = it) }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<ChannelInteractor> { interactor }
                }
            )
        }
        runBlocking {
            whenever(
                interactor.loadChannels(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                requests += LoadRequest(offset, query, loadKey?.value)

                val page = pageFor(offset)
                val hasNext = offset < 40
                flow {
                    emit(
                        PaginationResponse.DBResponse(
                            data = page,
                            loadKey = loadKey,
                            offset = offset,
                            hasNext = hasNext,
                            query = query
                        )
                    )
                    emit(
                        PaginationResponse.ServerResponse(
                            data = SceytResponse.Success(emptyList()),
                            cacheData = page,
                            loadKey = loadKey,
                            offset = offset,
                            hasDiff = false,
                            hasNext = hasNext,
                            hasPrev = false,
                            loadType = PaginationResponse.LoadType.LoadNext,
                            ignoredDb = false,
                            query = query
                        )
                    )
                    completedRequests += CompletedRequest(offset, query)
                }
            }
        }
    }

    @After
    fun tearDown() = runBlocking {
        Dispatchers.resetMain()
        channelsCache.clearAll()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `load more keeps db page offset after live insert before second page`() = runTest(dispatcher) {
        val viewModel = ChannelsViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        appendLiveChannel(viewModel, createChannel(id = 100, pinnedAt = 0, createdAt = 100))

        assertThat(viewModel.state.value.channels.size).isEqualTo(21)

        val firstLoadMoreLastChannelId = viewModel.state.value.channels.lastOrNull()?.id
        viewModel.loadMoreChannels(lastChannelId = firstLoadMoreLastChannelId)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20)
        }

        assertThat(requests.map { it.offset }).containsExactly(0, 20).inOrder()
        assertThat(requests[1].loadKeyValue).isEqualTo(firstLoadMoreLastChannelId)
    }

    @Test
    fun `subsequent load more keeps accumulated offset after multiple live inserts`() = runTest(dispatcher) {
        val viewModel = ChannelsViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        appendLiveChannel(viewModel, createChannel(id = 100, pinnedAt = 0, createdAt = 100))

        assertThat(viewModel.state.value.channels.size).isEqualTo(21)

        viewModel.loadMoreChannels(lastChannelId = viewModel.state.value.channels.lastOrNull()?.id)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20) &&
                    viewModel.state.value.channels.size == 41
        }

        appendLiveChannel(viewModel, createChannel(id = 200, pinnedAt = 0, createdAt = 200))

        assertThat(viewModel.state.value.channels.size).isEqualTo(42)

        val secondLoadMoreLastChannelId = viewModel.state.value.channels.lastOrNull()?.id
        viewModel.loadMoreChannels(lastChannelId = secondLoadMoreLastChannelId)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20, 40) &&
                    completedRequests.map { it.offset } == listOf(0, 20, 40)
        }

        assertThat(requests.map { it.offset }).containsExactly(0, 20, 40).inOrder()
        assertThat(requests.last().loadKeyValue).isEqualTo(secondLoadMoreLastChannelId)
    }

    @Test
    fun `late server responses do not rewind accumulated next offset`() = runTest(dispatcher) {
        val viewModel = ChannelsViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        dispatchPaginationResponse(
            viewModel,
            dbResponse(offset = 20, data = secondPage, hasNext = true)
        )
        dispatchPaginationResponse(
            viewModel,
            dbResponse(offset = 40, data = thirdPage, hasNext = true)
        )

        assertThat(viewModel.state.value.channels.size).isEqualTo(60)

        val fullCache = firstPage + secondPage + thirdPage
        dispatchPaginationResponse(
            viewModel,
            serverResponse(
                offset = 0,
                data = firstPage,
                cacheData = fullCache,
                hasDiff = false,
                hasNext = true,
            )
        )
        dispatchPaginationResponse(
            viewModel,
            serverResponse(
                offset = 20,
                data = secondPage,
                cacheData = fullCache,
                hasDiff = false,
                hasNext = true,
            )
        )

        val lastVisibleChannelId = viewModel.state.value.channels.lastOrNull()?.id
        viewModel.loadMoreChannels(lastVisibleChannelId)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 60) &&
                    completedRequests.map { it.offset } == listOf(0, 60)
        }

        assertThat(requests.last().offset).isEqualTo(60)
        assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
    }

    @Test
    fun `server diff replacement keeps paging aligned to loaded page size not rendered size`() =
        runTest(dispatcher) {
            val viewModel = ChannelsViewModel(config, dispatcher)
            awaitCondition {
                requests.map { it.offset } == listOf(0) &&
                        completedRequests.map { it.offset } == listOf(0) &&
                        viewModel.state.value.channels.size == 20
            }

            dispatchPaginationResponse(
                viewModel,
                dbResponse(offset = 20, data = secondPage, hasNext = true)
            )
            assertThat(viewModel.state.value.channels.size).isEqualTo(40)

            val insertedChannel = createChannel(id = 100, pinnedAt = 0, createdAt = 100)
            val replacedCache = (firstPage + secondPage + insertedChannel)
                .sortedWith(ChannelsComparatorDescBy(config.order))
            dispatchPaginationResponse(
                viewModel,
                serverResponse(
                    offset = 20,
                    data = secondPage,
                    cacheData = replacedCache,
                    hasDiff = true,
                    hasNext = true,
                )
            )

            assertThat(viewModel.state.value.channels.map { it.id })
                .containsExactlyElementsIn(replacedCache.map { it.id })
                .inOrder()

            val lastVisibleChannelId = viewModel.state.value.channels.lastOrNull()?.id
            viewModel.loadMoreChannels(lastVisibleChannelId)
            awaitCondition {
                requests.map { it.offset } == listOf(0, 40) &&
                        completedRequests.map { it.offset } == listOf(0, 40)
            }

            assertThat(requests.last().offset).isEqualTo(40)
            assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
        }

    @Test
    fun `new first page request resets accumulated next offset`() = runTest(dispatcher) {
        val viewModel = ChannelsViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        viewModel.loadMoreChannels(viewModel.state.value.channels.lastOrNull()?.id)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20) &&
                    viewModel.state.value.channels.size == 40
        }

        viewModel.getChannels(offset = 0, query = "alpha")
        awaitCondition {
            requests.size == 3 &&
                    completedRequests.size == 3 &&
                    requests[2].offset == 0 &&
                    requests[2].query == "alpha" &&
                    completedRequests[2].offset == 0 &&
                    completedRequests[2].query == "alpha" &&
                    viewModel.state.value.channels.size == 20
        }

        val lastVisibleChannelId = viewModel.state.value.channels.lastOrNull()?.id
        viewModel.loadMoreChannels(lastVisibleChannelId)
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20, 0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20, 0, 20) &&
                    requests.last().query == "alpha"
        }

        assertThat(requests.map { it.offset }).containsExactly(0, 20, 0, 20).inOrder()
        assertThat(requests.last().query).isEqualTo("alpha")
        assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
    }

    @Test
    fun `realtime message on unloaded channel reorders list but load more keeps db page offset`() =
        runTest(dispatcher) {
            val realtimeConfig = config.copy(order = ChannelListOrder.ListQueryChannelOrderLastMessage)
            val firstRealtimePage = (1L..20L).map { id ->
                createChannel(id, pinnedAt = 0, createdAt = id, lastMessage = createMessage(200 - id))
            }
            val secondRealtimePage = (21L..40L).map { id ->
                createChannel(id, pinnedAt = 0, createdAt = id, lastMessage = createMessage(200 - id))
            }
            val dbChannels = (firstRealtimePage + secondRealtimePage)
                .sortedWith(ChannelsComparatorDescBy(realtimeConfig.order))
                .toMutableList()
            val loadedCache = mutableListOf<SceytChannel>()

            requests.clear()
            completedRequests.clear()

            whenever(
                interactor.loadChannels(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                requests += LoadRequest(offset, query, loadKey?.value)

                val page = dbChannels.drop(offset).take(realtimeConfig.queryLimit)
                val hasNext = offset + page.size < dbChannels.size

                flow {
                    emit(
                        PaginationResponse.DBResponse(
                            data = page,
                            loadKey = loadKey,
                            offset = offset,
                            hasNext = hasNext,
                            query = query
                        )
                    )
                    loadedCache.mergeByIdSorted(page, realtimeConfig.order)
                    emit(
                        PaginationResponse.ServerResponse(
                            data = SceytResponse.Success(page),
                            cacheData = loadedCache.toList(),
                            loadKey = loadKey,
                            offset = offset,
                            hasDiff = true,
                            hasNext = hasNext,
                            hasPrev = false,
                            loadType = PaginationResponse.LoadType.LoadNext,
                            ignoredDb = false,
                            query = query
                        )
                    )
                    completedRequests += CompletedRequest(offset, query)
                }
            }

            val viewModel = ChannelsViewModel(realtimeConfig, dispatcher)
            advanceUntilIdle()
            assertThat(requests.map { it.offset }).containsExactly(0).inOrder()
            assertThat(completedRequests.map { it.offset }).containsExactly(0).inOrder()
            assertThat(viewModel.state.value.channels.map { it.id })
                .containsExactlyElementsIn(firstRealtimePage.map { it.id })
                .inOrder()

            channelsCache.addAll(realtimeConfig, firstRealtimePage, checkDifference = false)

            val bumpedChannel = secondRealtimePage.first { it.id == 30L }.copy(
                lastMessage = createMessage(1_000, id = 30)
            )
            dbChannels.removeAll { it.id == bumpedChannel.id }
            dbChannels += bumpedChannel
            dbChannels.sortWith(ChannelsComparatorDescBy(realtimeConfig.order))
            loadedCache.mergeByIdSorted(listOf(bumpedChannel), realtimeConfig.order)

            channelsCache.upsertChannel(bumpedChannel)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.size).isEqualTo(21)
            assertThat(viewModel.state.value.channels.firstOrNull()?.id).isEqualTo(30L)

            val lastVisibleChannelId = viewModel.state.value.channels.lastOrNull()?.id
            viewModel.loadMoreChannels(lastVisibleChannelId)
            advanceUntilIdle()

            assertThat(requests.map { it.offset }).containsExactly(0, 20).inOrder()
            assertThat(completedRequests.map { it.offset }).containsExactly(0, 20).inOrder()
            assertThat(requests.last().offset).isEqualTo(20)
            assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
            assertThat(viewModel.state.value.channels.map { it.id })
                .containsExactlyElementsIn(dbChannels.map { it.id })
                .inOrder()
        }

    @Test
    fun `all channels are eventually loaded when realtime messages reorder database between page loads`() =
        runTest(dispatcher) {
            val realtimeConfig = config.copy(order = ChannelListOrder.ListQueryChannelOrderLastMessage)
            val dbChannels = (1L..60L)
                .map { id ->
                    createChannel(
                        id,
                        pinnedAt = 0,
                        createdAt = id,
                        lastMessage = createMessage(1_000 - id, id = id)
                    )
                }
                .sortedWith(ChannelsComparatorDescBy(realtimeConfig.order))
                .toMutableList()
            val loadedCache = mutableListOf<SceytChannel>()

            requests.clear()
            completedRequests.clear()

            whenever(
                interactor.loadChannels(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { invocation ->
                val offset = invocation.getArgument<Int>(0)
                val query = invocation.getArgument<String>(1)
                val loadKey = invocation.getArgument<LoadKeyData?>(2)
                requests += LoadRequest(offset, query, loadKey?.value)

                val page = dbChannels.drop(offset).take(realtimeConfig.queryLimit)
                val hasNext = offset + page.size < dbChannels.size

                flow {
                    emit(
                        PaginationResponse.DBResponse(
                            data = page,
                            loadKey = loadKey,
                            offset = offset,
                            hasNext = hasNext,
                            query = query
                        )
                    )
                    loadedCache.mergeByIdSorted(page, realtimeConfig.order)
                    emit(
                        PaginationResponse.ServerResponse(
                            data = SceytResponse.Success(page),
                            cacheData = loadedCache.toList(),
                            loadKey = loadKey,
                            offset = offset,
                            hasDiff = true,
                            hasNext = hasNext,
                            hasPrev = false,
                            loadType = PaginationResponse.LoadType.LoadNext,
                            ignoredDb = false,
                            query = query
                        )
                    )
                    completedRequests += CompletedRequest(offset, query)
                }
            }

            val viewModel = ChannelsViewModel(realtimeConfig, dispatcher)
            advanceUntilIdle()

            val initialFirstPage = dbChannels.take(realtimeConfig.queryLimit)
            assertThat(requests.map { it.offset }).containsExactly(0).inOrder()
            assertThat(completedRequests.map { it.offset }).containsExactly(0).inOrder()
            assertThat(viewModel.state.value.channels.map { it.id })
                .containsExactlyElementsIn(initialFirstPage.map { it.id })
                .inOrder()

            channelsCache.addAll(realtimeConfig, initialFirstPage, checkDifference = false)

            val firstBumpedChannel = dbChannels.first { it.id == 45L }.copy(
                lastMessage = createMessage(10_000, id = 450)
            )
            dbChannels.removeAll { it.id == firstBumpedChannel.id }
            dbChannels += firstBumpedChannel
            dbChannels.sortWith(ChannelsComparatorDescBy(realtimeConfig.order))
            loadedCache.mergeByIdSorted(listOf(firstBumpedChannel), realtimeConfig.order)

            channelsCache.upsertChannel(firstBumpedChannel)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.firstOrNull()?.id).isEqualTo(45L)

            val lastChannelAfterFirstPage = viewModel.state.value.channels.lastOrNull()?.id
            viewModel.loadMoreChannels(lastChannelAfterFirstPage)
            advanceUntilIdle()

            assertThat(requests.map { it.offset }).containsExactly(0, 20).inOrder()
            assertThat(completedRequests.map { it.offset }).containsExactly(0, 20).inOrder()
            assertThat(viewModel.state.value.channels.map { it.id }.distinct())
                .hasSize(viewModel.state.value.channels.size)

            val secondBumpedChannel = dbChannels.first { it.id == 58L }.copy(
                lastMessage = createMessage(20_000, id = 580)
            )
            dbChannels.removeAll { it.id == secondBumpedChannel.id }
            dbChannels += secondBumpedChannel
            dbChannels.sortWith(ChannelsComparatorDescBy(realtimeConfig.order))
            loadedCache.mergeByIdSorted(listOf(secondBumpedChannel), realtimeConfig.order)

            channelsCache.upsertChannel(secondBumpedChannel)
            advanceUntilIdle()
            assertThat(viewModel.state.value.channels.firstOrNull()?.id).isEqualTo(58L)

            val lastChannelAfterSecondPage = viewModel.state.value.channels.lastOrNull()?.id
            viewModel.loadMoreChannels(lastChannelAfterSecondPage)
            advanceUntilIdle()

            assertThat(requests.map { it.offset }).containsExactly(0, 20, 40).inOrder()
            assertThat(completedRequests.map { it.offset }).containsExactly(0, 20, 40).inOrder()
            assertThat(viewModel.state.value.hasNext).isFalse()
            assertThat(viewModel.state.value.channels.map { it.id })
                .containsExactlyElementsIn(dbChannels.map { it.id })
                .inOrder()
            assertThat(viewModel.state.value.channels.map { it.id }.distinct()).hasSize(dbChannels.size)
            assertThat(viewModel.state.value.channels).hasSize(dbChannels.size)
        }

    private suspend fun TestScope.awaitCondition(
        maxSteps: Int = 200,
        condition: () -> Boolean,
    ) {
        repeat(maxSteps) {
            if (condition()) return
            advanceUntilIdle()
            yield()
        }
        if (condition()) return
        throw AssertionError("Condition was not met after $maxSteps scheduler steps")
    }

    private fun dispatchPaginationResponse(
        viewModel: ChannelsViewModel,
        response: PaginationResponse<SceytChannel>,
    ) {
        val method = ChannelsViewModel::class.java.getDeclaredMethod(
            "initPaginationResponse",
            PaginationResponse::class.java
        )
        method.isAccessible = true
        method.invoke(viewModel, response)
    }

    @Suppress("UNCHECKED_CAST")
    private fun appendLiveChannel(viewModel: ChannelsViewModel, channel: SceytChannel) {
        val stateField = ChannelsViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<ChannelListState>
        val current = stateFlow.value
        stateFlow.value = current.copy(
            channels = (current.channels + channel).sortedWith(ChannelsComparatorDescBy(config.order))
        )
    }

    private fun dbResponse(
        offset: Int,
        data: List<SceytChannel>,
        hasNext: Boolean,
        query: String = "",
    ) = PaginationResponse.DBResponse(
        data = data,
        loadKey = null,
        offset = offset,
        hasNext = hasNext,
        query = query
    )

    private fun serverResponse(
        offset: Int,
        data: List<SceytChannel>,
        cacheData: List<SceytChannel>,
        hasDiff: Boolean,
        hasNext: Boolean,
        query: String = "",
    ) = PaginationResponse.ServerResponse(
        data = SceytResponse.Success(data),
        cacheData = cacheData,
        loadKey = null,
        offset = offset,
        hasDiff = hasDiff,
        hasNext = hasNext,
        hasPrev = false,
        loadType = PaginationResponse.LoadType.LoadNext,
        ignoredDb = false,
        query = query
    )

    private fun MutableList<SceytChannel>.mergeByIdSorted(
        page: List<SceytChannel>,
        order: ChannelListOrder,
    ) {
        val merged = (this + page)
            .associateBy { it.id }
            .values
            .sortedWith(ChannelsComparatorDescBy(order))
        clear()
        addAll(merged)
    }

    private fun pageFor(offset: Int): List<SceytChannel> = when (offset) {
        0 -> firstPage
        20 -> secondPage
        40 -> thirdPage
        else -> emptyList()
    }

    private data class LoadRequest(
        val offset: Int,
        val query: String,
        val loadKeyValue: Long?,
    )

    private data class CompletedRequest(
        val offset: Int,
        val query: String,
    )
}
