package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.notifications.SceytNotifications
import com.sceyt.chatuikit.notifications.push.PushNotification
import com.sceyt.chatuikit.notifications.push.PushNotificationHandler
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.LoadingNextItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.LoadingPrevItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.UnreadMessagesSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.MessageListHeaderUIElementsListener
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.event.InputEventsListener
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageInputCommand
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.LoadKeyType
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.bind
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.messages_list.MessagesListHeaderStyle
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class MessageListViewModelStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val messageInteractor = mock<MessageInteractor>()
    private val channelInteractor = mock<ChannelInteractor>()
    private val channelMemberInteractor = mock<ChannelMemberInteractor>()
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
                messageInteractor.loadPrevMessages(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(0L)
                )
            ).thenReturn(emptyFlow())
            whenever(channelInteractor.getChannelFromServer(any()))
                .thenReturn(SceytResponse.Success(null))
            whenever(channelMemberInteractor.getMembersCountFromDb(any()))
                .thenReturn(1)
        }
        val notificationHandler = mock<PushNotificationHandler>()
        val pushNotification = mock<PushNotification>()
        whenever(pushNotification.notificationHandler).thenReturn(notificationHandler)
        val notifications = mock<SceytNotifications>()
        whenever(notifications.pushNotification).thenReturn(pushNotification)
        SceytChatUIKit.notifications = notifications
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<MessageInteractor> { messageInteractor }
                    single<ChannelInteractor> { channelInteractor }
                    single<MessageReactionInteractor> { mock() }
                    single<MessagePollInteractor> { mock() }
                    single<AttachmentInteractor> { mock() }
                    single<ChannelMemberInteractor> { channelMemberInteractor }
                    single<PersistenceConnectionLogic> {
                        mock {
                            on { allPendingEventsSentFlow } doReturn emptyFlow()
                        }
                    }
                    single<UserInteractor> { mock() }
                    single<SceytSyncManager> { mock() }
                    single<FileTransferService> { mock() }
                    single<PauseOrResumeTransferUseCase> { mock() }
                    single<SceytSharedPreference> { mock() }
                    single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { dispatcher }
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
            any(),
            eq(0L)
        )
    }

    @Test
    fun `initial target message load keeps unread separator boundary`() = runTest(dispatcher) {
        val lastDisplayedMessage = createMessage(createdAt = 1_000, id = 3, tid = 3)
        val targetMessage = createMessage(createdAt = 2_000, id = 4, tid = 4).copy(incoming = true)
        val lastMessage = createMessage(createdAt = 3_000, id = 5, tid = 5).copy(incoming = true)
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1, lastMessage = lastMessage)
            .copy(lastDisplayedMessageId = lastDisplayedMessage.id)
        val loadKey = LoadKeyData(
            key = LoadKeyType.ScrollToMessageBy.longValue,
            value = targetMessage.id
        )
        whenever(
            messageInteractor.loadNearMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(lastDisplayedMessage, targetMessage, lastMessage),
                    loadKey = loadKey,
                    offset = 0,
                    hasNext = false,
                    hasPrev = false,
                    loadType = LoadNear
                )
            )
        )

        val viewModel = viewModel(
            channel = channel,
            initialTargetMessageId = targetMessage.id
        )
        advanceUntilIdle()

        val separators = viewModel.state.value.items.filterIsInstance<UnreadMessagesSeparatorItem>()
        assertThat(separators).containsExactly(
            UnreadMessagesSeparatorItem(
                createdAt = targetMessage.createdAt,
                msgId = lastDisplayedMessage.id
            )
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
    fun `synced messages are not appended to load near window with next page`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val centeredMessage = createMessage(createdAt = 1_000, id = 100, tid = 100)
        whenever(
            messageInteractor.loadNearMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(centeredMessage),
                    loadKey = LoadKeyData(value = centeredMessage.id),
                    offset = 0,
                    hasNext = true,
                    hasPrev = false,
                    loadType = LoadNear
                )
            )
        )

        viewModel.loadNearMessages(
            messageId = centeredMessage.id,
            loadKey = LoadKeyData(value = centeredMessage.id),
            ignoreServer = true
        )
        advanceUntilIdle()

        val appended = viewModel.appendSyncedMessages(
            messages = listOf(createMessage(createdAt = 2_000, id = 200, tid = 200)),
            scrollToLastAfterAppend = false
        )

        assertThat(appended).isFalse()
        assertThat(viewModel.state.value.items).contains(LoadingNextItem)
        assertThat(
            viewModel.state.value.items
                .filterIsInstance<MessageItem>()
                .map { it.message.id }
        ).containsExactly(centeredMessage.id)
    }

    @Test
    fun `load newest replaces load near window with next gap`() = runTest(dispatcher) {
        val centeredMessage = createMessage(createdAt = 1_000, id = 100, tid = 100)
        val latestMessage = createMessage(createdAt = 2_000, id = 200, tid = 200)
        val viewModel = viewModel(
            channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1, lastMessage = latestMessage)
        )
        whenever(
            messageInteractor.loadNearMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(centeredMessage),
                    loadKey = LoadKeyData(value = centeredMessage.id),
                    offset = 0,
                    hasNext = true,
                    hasPrev = false,
                    loadType = LoadNear
                )
            )
        )
        viewModel.loadNearMessages(
            messageId = centeredMessage.id,
            loadKey = LoadKeyData(value = centeredMessage.id),
            ignoreServer = true
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.items).contains(LoadingNextItem)

        val requestId = 77L
        val loadKey = LoadKeyData(
            key = LoadKeyType.ScrollToLastMessage.longValue,
            value = latestMessage.id,
            data = ScrollRequestData(requestId)
        )
        whenever(
            messageInteractor.loadNewestMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.ServerResponse(
                    data = SceytResponse.Success(listOf(latestMessage)),
                    cacheData = listOf(latestMessage),
                    loadKey = loadKey,
                    offset = 0,
                    hasDiff = true,
                    hasNext = false,
                    hasPrev = true,
                    loadType = LoadNewest,
                    ignoredDb = false,
                    dbResultWasEmpty = true
                )
            )
        )
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.ScrollToLastMessage }
                    as MessageListRenderEffect.ScrollToLastMessage
        }
        runCurrent()

        viewModel.loadNewestMessages(loadKey)
        advanceUntilIdle()

        assertThat(effect.await().requestId).isEqualTo(requestId)
        assertThat(viewModel.state.value.items).doesNotContain(LoadingNextItem)
        assertThat(
            viewModel.state.value.items
                .filterIsInstance<MessageItem>()
                .map { it.message.id }
        ).containsExactly(latestMessage.id)
    }

    @Test
    fun `load prev server error keeps loading item retryable when db side is exhausted`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val message = createMessage(createdAt = 1_000, id = 100, tid = 100)
        val loadKey = LoadKeyData(value = message.id)
        whenever(
            messageInteractor.loadPrevMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(0L)
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(message),
                    loadKey = loadKey,
                    offset = 0,
                    hasPrev = true,
                    loadType = LoadPrev
                ),
                PaginationResponse.ServerResponse(
                    data = SceytResponse.Success(emptyList()),
                    cacheData = listOf(message),
                    loadKey = loadKey,
                    offset = 0,
                    hasDiff = false,
                    hasPrev = true,
                    hasNext = false,
                    loadType = LoadPrev,
                    ignoredDb = false,
                    dbResultWasEmpty = false
                )
            ),
            flowOf(
                PaginationResponse.DBResponse(
                    data = emptyList(),
                    loadKey = loadKey,
                    offset = 1,
                    hasPrev = false,
                    loadType = LoadPrev
                ),
                PaginationResponse.ServerResponse(
                    data = SceytResponse.Error(),
                    cacheData = emptyList(),
                    loadKey = loadKey,
                    offset = 1,
                    hasDiff = false,
                    hasPrev = true,
                    hasNext = false,
                    loadType = LoadPrev,
                    ignoredDb = false,
                    dbResultWasEmpty = true
                )
            )
        )

        viewModel.loadPrevMessages(lastMessageId = message.id, offset = 0, loadKey = loadKey)
        advanceUntilIdle()
        assertThat(viewModel.state.value.items).contains(LoadingPrevItem)

        viewModel.loadPrevMessages(lastMessageId = message.id, offset = 1, loadKey = loadKey)
        advanceUntilIdle()

        assertThat(viewModel.state.value.items).contains(LoadingPrevItem)
        assertThat(viewModel.canLoadPrev()).isTrue()
        assertThat(viewModel.canRetryLoadPrevAfterReconnect()).isTrue()
    }

    @Test
    fun `initial load prev server error stays retryable when db side is exhausted`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val message = createMessage(createdAt = 1_000, id = 100, tid = 100)
        val loadKey = LoadKeyData(value = message.id)
        whenever(
            messageInteractor.loadPrevMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(0L)
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(message),
                    loadKey = loadKey,
                    offset = 0,
                    hasPrev = false,
                    loadType = LoadPrev
                ),
                PaginationResponse.ServerResponse(
                    data = SceytResponse.Error(),
                    cacheData = listOf(message),
                    loadKey = loadKey,
                    offset = 0,
                    hasDiff = false,
                    hasPrev = false,
                    hasNext = false,
                    loadType = LoadPrev,
                    ignoredDb = false,
                    dbResultWasEmpty = false
                )
            )
        )

        viewModel.loadPrevMessages(lastMessageId = message.id, offset = 0, loadKey = loadKey)
        advanceUntilIdle()

        assertThat(viewModel.canLoadPrev()).isFalse()
        assertThat(viewModel.canRetryLoadPrevAfterReconnect()).isTrue()
        assertThat(viewModel.canRetryLoadNextAfterReconnect()).isFalse()
    }

    @Test
    fun `load near scroll response preserves scroll request id`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1_000, id = 100, tid = 100)
        val loadKey = LoadKeyData(
            key = LoadKeyType.ScrollToMessageBy.longValue,
            value = message.id,
            data = ScrollRequestData(requestId = 42)
        )
        whenever(
            messageInteractor.loadNearMessages(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(
            flowOf(
                PaginationResponse.DBResponse(
                    data = listOf(message),
                    loadKey = loadKey,
                    offset = 0,
                    hasNext = false,
                    hasPrev = false,
                    loadType = LoadNear
                )
            )
        )
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.ScrollToMessage }
                    as MessageListRenderEffect.ScrollToMessage
        }
        runCurrent()

        viewModel.loadNearMessages(
            messageId = message.id,
            loadKey = loadKey,
            ignoreServer = true
        )

        assertThat(effect.await().requestId).isEqualTo(42)
    }

    @Test
    fun `center sync response is not emitted after request invalidated`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1_000, id = 100, tid = 100)
        whenever(messageInteractor.syncNearMessages(any(), any(), any()))
            .thenReturn(
                SyncNearMessagesResult(
                    centerMessageId = message.id,
                    response = SceytResponse.Success(data = emptyList()),
                    missingMessages = emptyList()
                )
            )
        val effects = mutableListOf<MessageListRenderEffect>()
        val job = launch { viewModel.renderEffects.collect { effects.add(it) } }
        runCurrent()

        viewModel.syncCenteredMessage(message.id)
        viewModel.invalidateCenteredSync()
        advanceUntilIdle()
        job.cancel()

        assertThat(effects.filterIsInstance<MessageListRenderEffect.ApplyCenteredSync>()).isEmpty()
    }

    @Test
    fun `center sync response is emitted as an ApplyCenteredSync effect`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1_000, id = 100, tid = 100)
        whenever(messageInteractor.syncNearMessages(any(), any(), any()))
            .thenReturn(
                SyncNearMessagesResult(
                    centerMessageId = message.id,
                    response = SceytResponse.Success(data = emptyList()),
                    missingMessages = emptyList()
                )
            )
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.ApplyCenteredSync }
                    as MessageListRenderEffect.ApplyCenteredSync
        }
        runCurrent()

        viewModel.syncCenteredMessage(message.id)

        assertThat(effect.await().result.data.centerMessageId).isEqualTo(message.id)
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
    fun `messages list binding restores multiselect mode from retained selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1).copy(isSelected = true)
        viewModel.selectedMessagesMap[message.tid] = message
        val messagesListView = messagesListView()

        viewModel.bind(messagesListView, TestLifecycleOwner())
        runCurrent()

        verify(messagesListView).setMultiSelectableMode()
    }

    @Test
    fun `messages list binding multiselect event enters mode and retains selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)
        var commandListener: ((MessageCommandEvent) -> Unit)? = null
        val messagesListView = messagesListView { commandListener = it }
        viewModel.bind(messagesListView, TestLifecycleOwner())
        runCurrent()

        commandListener?.invoke(MessageCommandEvent.MultiselectEvent(message))
        runCurrent()

        assertThat(viewModel.selectedMessagesMap.keys).containsExactly(message.tid)
        verify(messagesListView).setMultiSelectableMode()
    }

    @Test
    fun `messages list binding cancel event clears retained selection and exits mode`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1).copy(isSelected = true)
        viewModel.selectedMessagesMap[message.tid] = message
        var commandListener: ((MessageCommandEvent) -> Unit)? = null
        val messagesListView = messagesListView { commandListener = it }
        viewModel.bind(messagesListView, TestLifecycleOwner())
        runCurrent()
        clearInvocations(messagesListView)

        commandListener?.invoke(MessageCommandEvent.CancelMultiselectEvent)
        runCurrent()

        assertThat(viewModel.selectedMessagesMap).isEmpty()
        verify(messagesListView).cancelMultiSelectMode()
    }

    @Test
    fun `header binding restores actions menu from retained selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1).copy(isSelected = true)
        viewModel.selectedMessagesMap[message.tid] = message
        val uiElements = RecordingHeaderUiElements()
        val headerView = headerView(uiElements)

        viewModel.bind(headerView, replyInThreadMessage = null, lifecycleOwner = TestLifecycleOwner())
        runCurrent()

        assertThat(uiElements.shownMessages.map { it.tid }).containsExactly(message.tid)
    }

    @Test
    fun `input binding restores multiselect mode from retained selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1).copy(isSelected = true)
        viewModel.selectedMessagesMap[message.tid] = message
        val inputEvents = RecordingInputEvents()
        val inputView = inputView(inputEvents)

        viewModel.bind(inputView, replyInThreadMessage = null, lifecycleOwner = TestLifecycleOwner())
        runCurrent()

        assertThat(inputEvents.multiselectModes).contains(true)
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

    @Test
    fun `prepareToScrollToReplyMessage emits ScrollToMessage for the parent with highlight`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val parent = createMessage(createdAt = 1, id = 1, tid = 1)
            val reply = createMessage(createdAt = 2, id = 2, tid = 2).copy(parentMessage = parent)
            val effect = async {
                viewModel.renderEffects.first { it is MessageListRenderEffect.ScrollToMessage }
                        as MessageListRenderEffect.ScrollToMessage
            }
            runCurrent()

            viewModel.prepareToScrollToReplyMessage(reply)

            val scroll = effect.await()
            assertThat(scroll.messageId).isEqualTo(parent.id)
            assertThat(scroll.highlight).isTrue()
            assertThat(scroll.offset).isEqualTo(200)
            assertThat(scroll.loadOnMissing?.loadKey)
                    .isEqualTo(LoadKeyType.ScrollToReplyMessage.longValue)
            assertThat(scroll.loadOnMissing?.ignoreServer).isFalse()
        }

    @Test
    fun `prepareToScrollToReplyMessage without a parent emits nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val withoutParent = createMessage(createdAt = 1, id = 1, tid = 1)
        val withParent = createMessage(createdAt = 2, id = 2, tid = 2)
                .copy(parentMessage = createMessage(createdAt = 3, id = 3, tid = 3))
        val effect = async {
            viewModel.renderEffects.first { it is MessageListRenderEffect.ScrollToMessage }
                    as MessageListRenderEffect.ScrollToMessage
        }
        runCurrent()

        // No parent -> early return, no effect. The following call is the first to emit one.
        viewModel.prepareToScrollToReplyMessage(withoutParent)
        viewModel.prepareToScrollToReplyMessage(withParent)

        assertThat(effect.await().messageId).isEqualTo(3L)
    }

    // Rotation safety: reply-message scroll is a one-shot render effect. A collector that
    // re-subscribes after rotation must not receive a previously emitted scroll/highlight command.
    @Test
    fun `prepareToScrollToReplyMessage does not replay to a late subscriber`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val oldParent = createMessage(createdAt = 1, id = 1, tid = 1)
        val oldReply = createMessage(createdAt = 2, id = 2, tid = 2).copy(parentMessage = oldParent)
        viewModel.prepareToScrollToReplyMessage(oldReply)
        advanceUntilIdle()

        val effects = mutableListOf<MessageListRenderEffect.ScrollToMessage>()
        val job = launch {
            viewModel.renderEffects.collect {
                if (it is MessageListRenderEffect.ScrollToMessage) effects.add(it)
            }
        }
        runCurrent()

        assertThat(effects).isEmpty()

        val newParent = createMessage(createdAt = 3, id = 3, tid = 3)
        val newReply = createMessage(createdAt = 4, id = 4, tid = 4).copy(parentMessage = newParent)
        viewModel.prepareToScrollToReplyMessage(newReply)
        advanceUntilIdle()
        job.cancel()

        assertThat(effects.single().messageId).isEqualTo(newParent.id)
    }

    @Test
    fun `prepareToScrollToNewMessage emits ScrollToNewMessage with the channel last message`() =
        runTest(dispatcher) {
            val lastMessage = createMessage(createdAt = 10, id = 10, tid = 10)
            val viewModel = viewModel(
                channel = createChannel(
                    id = 1,
                    pinnedAt = 0,
                    createdAt = 1,
                    lastMessage = lastMessage
                )
            )
            val effect = async {
                viewModel.renderEffects.first { it is MessageListRenderEffect.ScrollToNewMessage }
                        as MessageListRenderEffect.ScrollToNewMessage
            }
            runCurrent()

            viewModel.prepareToScrollToNewMessage()

            assertThat(effect.await().lastMessage?.id).isEqualTo(lastMessage.id)
        }

    @Test
    fun `prepareToEditMessage emits an Edit input command`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)
        val command = async { viewModel.inputCommands.first() }
        runCurrent()

        viewModel.prepareToEditMessage(message)

        val edit = command.await() as MessageInputCommand.Edit
        assertThat(edit.message.id).isEqualTo(1L)
    }

    @Test
    fun `prepareToReplyMessage emits a Reply input command`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val message = createMessage(createdAt = 1, id = 1, tid = 1)
        val command = async { viewModel.inputCommands.first() }
        runCurrent()

        viewModel.prepareToReplyMessage(message)

        val reply = command.await() as MessageInputCommand.Reply
        assertThat(reply.message.id).isEqualTo(1L)
    }

    // Rotation safety: input commands are one-shot. A collector that re-subscribes after a
    // configuration change must not be re-delivered a command emitted before it subscribed.
    @Test
    fun `inputCommands does not replay to a late subscriber`() = runTest(dispatcher) {
        val viewModel = viewModel()
        // Emitted while nothing is observing (e.g. before a rotated view re-subscribes).
        viewModel.prepareToEditMessage(createMessage(createdAt = 1, id = 1, tid = 1))
        advanceUntilIdle()

        val received = mutableListOf<MessageInputCommand>()
        val job = launch { viewModel.inputCommands.collect { received.add(it) } }
        runCurrent()

        // The late subscriber must NOT receive the pre-subscription command.
        assertThat(received).isEmpty()

        // It still receives commands emitted after it subscribed.
        viewModel.prepareToReplyMessage(createMessage(createdAt = 2, id = 2, tid = 2))
        advanceUntilIdle()
        job.cancel()

        assertThat(received.single()).isInstanceOf(MessageInputCommand.Reply::class.java)
    }

    private fun viewModel(
        channel: SceytChannel = createChannel(id = 1, pinnedAt = 0, createdAt = 1),
        initialTargetMessageId: Long? = null,
    ): MessageListViewModel {
        return MessageListViewModel(
            _conversationId = channel.id,
            _channel = channel,
            replyInThread = false,
            initialTargetMessageId = initialTargetMessageId,
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

    private fun messagesListView(
        commandListener: (((MessageCommandEvent) -> Unit) -> Unit)? = null,
    ): MessagesListView {
        val style = mock<MessagesListViewStyle> {
            on { enableDateSeparator } doReturn false
        }
        return mock<MessagesListView>().also { view ->
            whenever(view.style).thenReturn(style)
            commandListener?.let { capture ->
                doAnswer { invocation ->
                    @Suppress("UNCHECKED_CAST")
                    capture(invocation.getArgument(0) as (MessageCommandEvent) -> Unit)
                    null
                }.whenever(view).setMessageCommandEventListener(any())
            }
        }
    }

    private fun headerView(uiElements: MessageListHeaderUIElementsListener.ElementsListeners): MessagesListHeaderView {
        val style = mock<MessagesListHeaderStyle> {
            on { messageActionsMenuStyle } doReturn MenuStyle()
        }
        return mock<MessagesListHeaderView>().also { view ->
            whenever(view.uiElementsListeners).thenReturn(uiElements)
            whenever(view.style).thenReturn(style)
        }
    }

    private fun inputView(inputEvents: InputEventsListener.InputEventListeners): MessageInputView {
        return mock<MessageInputView>().also { view ->
            whenever(view.getEventListeners()).thenReturn(inputEvents)
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry

        init {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
    }

    private class RecordingHeaderUiElements : MessageListHeaderUIElementsListener.ElementsListeners {
        val shownMessages = mutableListOf<SceytMessage>()

        override fun onTitle(
            titleTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage?,
            replyInThread: Boolean,
        ) = Unit

        override fun onSubTitle(
            subjectTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage?,
            replyInThread: Boolean,
        ) = Unit

        override fun onAvatar(
            avatar: AvatarView,
            channel: SceytChannel,
            replyInThread: Boolean,
        ) = Unit

        override fun onShowMessageActionsMenu(
            vararg messages: SceytMessage,
            menuStyle: MenuStyle,
            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?,
        ) {
            shownMessages.clear()
            shownMessages.addAll(messages)
        }

        override fun onHideMessageActionsMenu() = Unit

        override fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu) = Unit

        override fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages) = Unit
    }

    private class RecordingInputEvents : InputEventsListener.InputEventListeners {
        val multiselectModes = mutableListOf<Boolean>()

        override fun onInputStateChanged(sendImage: ImageView, state: InputState) = Unit

        override fun onMentionUsersListener(query: String) = Unit

        override fun onMultiselectModeListener(isMultiselectMode: Boolean) {
            multiselectModes.add(isMultiselectMode)
        }

        override fun onSearchModeChangeListener(inSearchMode: Boolean) = Unit
    }
}
