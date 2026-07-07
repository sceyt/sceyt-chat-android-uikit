package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageListViewModelMentionTest {
    private val dispatcher = StandardTestDispatcher()
    private val messageInteractor = mock<MessageInteractor>()
    private val userInteractor = mock<UserInteractor>()
    private val chatUIFacade = mock<SceytChatUIFacade>()
    private val onMessageFlow = MutableSharedFlow<Pair<SceytChannel, SceytMessage>>(extraBufferCapacity = 8)
    private val createdViewModels = mutableListOf<MessageListViewModel>()
    private val myId = "me"

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopKoin()
        whenever(messageInteractor.getOnMessageFlow()).thenReturn(onMessageFlow)
        // loadInitialMessages() runs in the VM init; stub the loaders so construction's load is a no-op.
        runBlocking {
            whenever(
                messageInteractor.loadPrevMessages(any(), any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(emptyFlow())
        }
        whenever(userInteractor.getCurrentUserId()).thenReturn(myId)
        whenever(chatUIFacade.userInteractor).thenReturn(userInteractor)
        whenever(chatUIFacade.myId).thenReturn(myId)

        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<MessageInteractor> { messageInteractor }
                    single<UserInteractor> { userInteractor }
                    single<SceytChatUIFacade> { chatUIFacade }
                    single<ChannelInteractor> { mock() }
                    single<MessageReactionInteractor> { mock() }
                    single<MessagePollInteractor> { mock() }
                    single<AttachmentInteractor> { mock() }
                    single<ChannelMemberInteractor> { mock() }
                    single<PersistenceConnectionLogic> { mock() }
                    single<SceytSyncManager> { mock() }
                    single<FileTransferService> { mock() }
                    single<PauseOrResumeTransferUseCase> { mock() }
                    single<SceytSharedPreference> { mock() }
                }
            )
        }
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `re-delivered mention message bumps the mention count only once`() = runTest(dispatcher) {
        val viewModel = viewModel()
        // onNewMessageFlow side effects only run while the flow is collected.
        val job = launch { viewModel.onNewMessageFlow.collect() }
        advanceUntilIdle()

        val message = mentionMessage(id = 1, tid = 1)
        onMessageFlow.emit(viewModel.channel to message)
        advanceUntilIdle()
        // Same message id delivered again (e.g. re-sync / duplicate realtime event).
        onMessageFlow.emit(viewModel.channel to message.copy(body = "edited"))
        advanceUntilIdle()
        job.cancel()

        assertThat(viewModel.channel.newMentionCount).isEqualTo(1)
    }

    @Test
    fun `distinct mention messages each bump the mention count`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val job = launch { viewModel.onNewMessageFlow.collect() }
        advanceUntilIdle()

        onMessageFlow.emit(viewModel.channel to mentionMessage(id = 1, tid = 1))
        advanceUntilIdle()
        onMessageFlow.emit(viewModel.channel to mentionMessage(id = 2, tid = 2))
        advanceUntilIdle()
        job.cancel()

        assertThat(viewModel.channel.newMentionCount).isEqualTo(2)
    }

    private fun mentionMessage(id: Long, tid: Long): SceytMessage =
        createMessage(createdAt = id, id = id, tid = tid).copy(
            incoming = true,
            displayCount = 1,
            mentionedUsers = listOf(createEmptyUser(myId))
        )

    private fun viewModel(): MessageListViewModel {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)
        return MessageListViewModel(
            _conversationId = channel.id,
            _channel = channel,
            replyInThread = false,
            initialTargetMessageId = null,
            ioDispatcher = dispatcher,
            defaultDispatcher = dispatcher,
            mainDispatcher = dispatcher,
            editedOrDeletedMessagesFlow = MutableSharedFlow(),
            outgoingMessagesFlow = MutableSharedFlow(),
            channelEventsFlow = MutableSharedFlow(),
            channelMemberActivityEventsFlow = MutableSharedFlow(),
            channelMembersEventsFlow = MutableSharedFlow(),
        ).also {
            it.configureMessageList(enableDateSeparator = false)
            createdViewModels.add(it)
        }
    }
}
