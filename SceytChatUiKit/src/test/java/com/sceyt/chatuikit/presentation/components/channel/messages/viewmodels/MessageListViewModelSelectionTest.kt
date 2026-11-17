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
import com.sceyt.chatuikit.persistence.interactor.MessagePinInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class MessageListViewModelSelectionTest {
    private val dispatcher = StandardTestDispatcher()
    private val messageInteractor = mock<MessageInteractor>()
    private val userInteractor = mock<UserInteractor>()
    private val chatUIFacade = mock<SceytChatUIFacade>()
    private val onMessageFlow = MutableSharedFlow<Pair<SceytChannel, SceytMessage>>(extraBufferCapacity = 8)
    private val createdViewModels = mutableListOf<MessageListViewModel>()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopKoin()
        whenever(messageInteractor.getOnMessageFlow()).thenReturn(onMessageFlow)
        runBlocking {
            whenever(
                messageInteractor.loadPrevMessages(any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(emptyFlow())
        }
        whenever(userInteractor.getCurrentUserId()).thenReturn("me")
        whenever(chatUIFacade.userInteractor).thenReturn(userInteractor)
        whenever(chatUIFacade.myId).thenReturn("me")

        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<MessageInteractor> { messageInteractor }
                    single<UserInteractor> { userInteractor }
                    single<SceytChatUIFacade> { chatUIFacade }
                    single<ChannelInteractor> { mock() }
                    single<MessageReactionInteractor> { mock() }
                    single<MessagePollInteractor> { mock() }
                    single<MessagePinInteractor> { mock() }
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
    fun `dropping one of several selected messages refreshes actions for the rest`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val kept = message(tid = 2)
        viewModel.selectedMessagesMap[1] = message(tid = 1)
        viewModel.selectedMessagesMap[2] = kept
        val effects = collectEffects(viewModel)

        viewModel.dropFromSelection(listOf(1L))

        assertThat(viewModel.selectedMessagesMap.keys).containsExactly(2L)
        assertThat(effects).containsExactly(MessageActionBridge.Effect.MessageActionsShown(listOf(kept)))
    }

    @Test
    fun `dropping the last selected message cancels multiselect`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.selectedMessagesMap[1] = message(tid = 1)
        val effects = collectEffects(viewModel)

        viewModel.dropFromSelection(listOf(1L))

        assertThat(viewModel.selectedMessagesMap).isEmpty()
        assertThat(effects).containsExactly(MessageActionBridge.Effect.MultiSelectCanceled)
    }

    @Test
    fun `dropping a message that is not selected changes nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.selectedMessagesMap[1] = message(tid = 1)
        val effects = collectEffects(viewModel)

        viewModel.dropFromSelection(listOf(5L))

        assertThat(viewModel.selectedMessagesMap.keys).containsExactly(1L)
        assertThat(effects).isEmpty()
    }

    private fun TestScope.collectEffects(viewModel: MessageListViewModel): List<MessageActionBridge.Effect> {
        val effects = mutableListOf<MessageActionBridge.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messageActionBridge.effects.toList(effects)
        }
        return effects
    }

    private fun message(tid: Long): SceytMessage = createMessage(createdAt = tid, id = tid, tid = tid)

    private fun viewModel(): MessageListViewModel {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)
        return MessageListViewModel(
            _conversationId = channel.id,
            _channel = channel,
            replyInThread = false,
            initialTargetMessageId = null,
        ).also {
            createdViewModels.add(it)
        }
    }
}
