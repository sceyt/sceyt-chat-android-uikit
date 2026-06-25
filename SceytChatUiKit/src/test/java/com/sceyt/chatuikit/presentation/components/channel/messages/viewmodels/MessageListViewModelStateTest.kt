package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
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
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageListViewModelStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val messageInteractor = mock<MessageInteractor>()
    private val createdViewModels = mutableListOf<MessageListViewModel>()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopKoin()
        whenever(messageInteractor.getOnMessageFlow()).thenReturn(MutableSharedFlow())
        // loadInitialMessages() runs in the VM init; stub the loaders so construction's load is a no-op.
        runBlocking {
            whenever(
                messageInteractor.loadPrevMessages(any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(emptyFlow())
        }
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<MessageInteractor> { messageInteractor }
                    single<ChannelInteractor> { mock() }
                    single<MessageReactionInteractor> { mock() }
                    single<MessagePollInteractor> { mock() }
                    single<AttachmentInteractor> { mock() }
                    single<ChannelMemberInteractor> { mock() }
                    single<PersistenceConnectionLogic> { mock() }
                    single<UserInteractor> { mock() }
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
    fun `append incoming message updates retained state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)

        viewModel.appendIncomingMessage(message)

        val item = viewModel.state.value.items.single() as MessageItem
        assertThat(item.message.tid).isEqualTo(1)
        assertThat(viewModel.state.value.hasLoadedInitialMessages).isTrue()
    }

    @Test
    fun `initial messages are loaded once on construction`() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        verify(messageInteractor, times(1)).loadPrevMessages(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `selection and expanded body are retained in state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)
        viewModel.appendIncomingMessage(message)

        viewModel.updateMessageSelection(message.copy(isSelected = true))
        viewModel.expandMessageBody(1)

        val item = viewModel.state.value.items.single() as MessageItem
        assertThat(item.message.isSelected).isTrue()
        assertThat(item.message.isBodyExpanded).isTrue()
    }

    @Test
    fun `duplicate realtime message is ignored`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)

        val firstAdd = viewModel.appendIncomingMessage(message)
        val duplicateAdd = viewModel.appendIncomingMessage(message.copy(body = "updated"))

        assertThat(firstAdd).isTrue()
        assertThat(duplicateAdd).isFalse()
        assertThat(viewModel.state.value.items.filterIsInstance<MessageItem>()).hasSize(1)
    }

    @Test
    fun `concurrent same day realtime appends keep one date separator`() = runTest(dispatcher) {
        val viewModel = viewModel().apply {
            configureMessageList(enableDateSeparator = true)
        }
        val messages = (1L..10L).map { id ->
            createMessage(createdAt = 1_000L + id, id = id, tid = id)
        }

        messages.map { message ->
            async { viewModel.appendIncomingMessage(message) }
        }.forEach { it.await() }

        val dateSeparators = viewModel.state.value.items.filterIsInstance<DateSeparatorItem>()
        val messageItems = viewModel.state.value.items.filterIsInstance<MessageItem>()
        assertThat(dateSeparators).hasSize(1)
        assertThat(messageItems.map { it.message.id }).containsExactlyElementsIn(messages.map { it.id })
    }

    @Test
    fun `incoming realtime append requests scroll only if already at end`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.AppendRealtime }
                    as MessageListRenderEffect.AppendRealtime
        }
        runCurrent()

        viewModel.appendIncomingMessage(createMessage(createdAt = 1, id = 1, tid = 1))

        assertThat(effect.await().scroll).isEqualTo(AppendRealtimeScroll.IfAtEnd)
    }

    @Test
    fun `outgoing realtime append always requests scroll to end`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.AppendRealtime }
                    as MessageListRenderEffect.AppendRealtime
        }
        runCurrent()

        viewModel.appendOutgoingMessage(createMessage(createdAt = 1, id = 1, tid = 1))

        assertThat(effect.await().scroll).isEqualTo(AppendRealtimeScroll.Always)
    }

    @Test
    fun `delete by tid removes matching message from retained state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.appendIncomingMessage(createMessage(createdAt = 1, id = 1, tid = 1))
        viewModel.appendIncomingMessage(createMessage(createdAt = 2, id = 2, tid = 2))

        viewModel.deleteMessagesByTid(1)

        val messages = viewModel.state.value.items.filterIsInstance<MessageItem>().map { it.message.tid }
        assertThat(messages).containsExactly(2L)
    }

    @Test
    fun `clear messages empties retained state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.appendIncomingMessage(createMessage(createdAt = 1, id = 1, tid = 1))

        viewModel.clearMessages()

        assertThat(viewModel.state.value.items).isEmpty()
        assertThat(viewModel.state.value.hasLoadedInitialMessages).isTrue()
    }

    @Test
    fun `clear selection updates all retained message items`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val first = createMessage(createdAt = 1, id = 1, tid = 1)
        val second = createMessage(createdAt = 2, id = 2, tid = 2)
        viewModel.appendIncomingMessage(first)
        viewModel.appendIncomingMessage(second)
        viewModel.updateMessageSelection(first.copy(isSelected = true))
        viewModel.updateMessageSelection(second.copy(isSelected = true))

        viewModel.clearMessageSelectionState()

        val selected = viewModel.state.value.items
            .filterIsInstance<MessageItem>()
            .filter { it.message.isSelected }
        assertThat(selected).isEmpty()
        assertThat(viewModel.selectedMessagesMap).isEmpty()
    }

    @Test
    fun `soft delete update emits null diff to keep delete animation`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)
        viewModel.appendIncomingMessage(message)
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.UpdateItem }
                    as MessageListRenderEffect.UpdateItem
        }
        runCurrent()

        viewModel.messageEditedOrDeleted(message.copy(state = MessageState.Deleted))

        assertThat(effect.await().diff).isNull()
    }

    @Test
    fun `center merge does not duplicate date separator for same day gap`() = runTest(dispatcher) {
        val viewModel = viewModel().apply {
            configureMessageList(enableDateSeparator = true)
        }
        val existingFirst = createMessage(createdAt = 1_000, id = 100, tid = 100)
        val existingSecond = createMessage(createdAt = 2_000, id = 200, tid = 200)
        val missingMessages = listOf(
            createMessage(createdAt = 100, id = 10, tid = 10),
            createMessage(createdAt = 200, id = 20, tid = 20)
        )
        viewModel.appendIncomingMessage(existingFirst)
        viewModel.appendIncomingMessage(existingSecond)

        viewModel.mergeMissingMessagesAroundCenter(
            data = SyncNearMessagesResult(
                centerMessageId = existingFirst.id,
                response = SceytResponse.Success(data = emptyList()),
                missingMessages = missingMessages
            ),
            topOffset = 0
        )

        val dateSeparators = viewModel.state.value.items.filterIsInstance<DateSeparatorItem>()
        val messages = viewModel.state.value.items.filterIsInstance<MessageItem>()
        assertThat(dateSeparators).hasSize(1)
        assertThat(messages.map { it.message.id }).containsExactly(10L, 20L, 100L, 200L).inOrder()
    }

    @Test
    fun `center merge keeps date separators for different day gap`() = runTest(dispatcher) {
        val viewModel = viewModel().apply {
            configureMessageList(enableDateSeparator = true)
        }
        val existing = createMessage(createdAt = 86_401_000, id = 100, tid = 100)
        val missingMessages = listOf(
            createMessage(createdAt = 1_000, id = 10, tid = 10),
            createMessage(createdAt = 2_000, id = 20, tid = 20)
        )
        viewModel.appendIncomingMessage(existing)

        viewModel.mergeMissingMessagesAroundCenter(
            data = SyncNearMessagesResult(
                centerMessageId = existing.id,
                response = SceytResponse.Success(data = emptyList()),
                missingMessages = missingMessages
            ),
            topOffset = 0
        )

        val dateSeparators = viewModel.state.value.items.filterIsInstance<DateSeparatorItem>()
        val messages = viewModel.state.value.items.filterIsInstance<MessageItem>()
        assertThat(dateSeparators).hasSize(2)
        assertThat(messages.map { it.message.id }).containsExactly(10L, 20L, 100L).inOrder()
    }

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
