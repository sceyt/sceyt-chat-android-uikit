package com.sceyt.chatuikit.presentation.components.shareable.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
class ShareableViewModelPaginationTest {
    private val dispatcher = StandardTestDispatcher()
    private val requests = mutableListOf<LoadRequest>()
    private val completedRequests = mutableListOf<CompletedRequest>()
    private val interactor = mock<ChannelInteractor>()
    private val config = ChannelListConfig(
        types = emptyList(),
        order = ChannelListOrder.ListQueryChannelOrderCreatedAt,
        queryLimit = 20,
        queryParam = ChannelQueryParam(1, 10, 1, true)
    )
    private val firstPage = (1L..20L).map { createChannel(it, pinnedAt = 0, createdAt = it) }
    private val secondPage = (21L..40L).map { createChannel(it, pinnedAt = 0, createdAt = it) }

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
                val hasNext = offset < 20
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
    fun tearDown() {
        Dispatchers.resetMain()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `load more uses stored offset even when rendered shareable list grows`() = runTest(dispatcher) {
        val viewModel = ShareableViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        appendVisibleChannel(viewModel, createChannel(id = 100, pinnedAt = 0, createdAt = 100))
        assertThat(viewModel.state.value.channels.size).isEqualTo(21)

        val lastVisibleChannelId = viewModel.state.value.channels.lastOrNull()?.id
        viewModel.loadMoreChannels()
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20)
        }

        assertThat(requests.map { it.offset }).containsExactly(0, 20).inOrder()
        assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
    }

    @Test
    fun `new search resets shareable pagination before next load more`() = runTest(dispatcher) {
        val viewModel = ShareableViewModel(config, dispatcher)
        awaitCondition {
            requests.map { it.offset } == listOf(0) &&
                    completedRequests.map { it.offset } == listOf(0) &&
                    viewModel.state.value.channels.size == 20
        }

        viewModel.loadMoreChannels()
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20) &&
                    viewModel.state.value.channels.size == 40
        }

        viewModel.onSearchQueryChanged("alpha")
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
        viewModel.loadMoreChannels()
        awaitCondition {
            requests.map { it.offset } == listOf(0, 20, 0, 20) &&
                    completedRequests.map { it.offset } == listOf(0, 20, 0, 20) &&
                    requests.last().query == "alpha"
        }

        assertThat(requests.map { it.offset }).containsExactly(0, 20, 0, 20).inOrder()
        assertThat(requests.last().query).isEqualTo("alpha")
        assertThat(requests.last().loadKeyValue).isEqualTo(lastVisibleChannelId)
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

    @Suppress("UNCHECKED_CAST")
    private fun appendVisibleChannel(viewModel: ShareableViewModel, channel: SceytChannel) {
        val stateField = ShareableViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<ChannelListState>
        val current = stateFlow.value
        stateFlow.value = current.copy(
            channels = (current.channels + channel).sortedWith(ChannelsComparatorDescBy(config.order))
        )
    }

    private fun pageFor(offset: Int): List<SceytChannel> = when (offset) {
        0 -> firstPage
        20 -> secondPage
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
