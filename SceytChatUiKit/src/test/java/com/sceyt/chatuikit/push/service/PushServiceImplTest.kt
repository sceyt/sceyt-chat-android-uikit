package com.sceyt.chatuikit.push.service

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkContinuation
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.settings.PushSubscription
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.constants.SceytConstants.SCEYT_WORKER_TAG
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorker
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushServiceType
import com.sceyt.chatuikit.push.providers.PushDeviceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PushServiceImplTest {
    private val context = mock<Context>()
    private val chatClient = mock<ChatClient>()
    private val messagesLogic = mock<PersistenceMessagesLogic>()
    private lateinit var chatClientStaticMock: MockedStatic<ChatClient>

    @Before
    fun setUp() {
        SceytChatUIKit.config = SceytChatUIKitConfig()
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        chatClientStaticMock = mockStatic()
        chatClientStaticMock.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(chatClient)
        whenever(chatClient.pushSubscriptions).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        WorkManagerImpl.setDelegate(null)
        chatClientStaticMock.close()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
        SceytChatUIKit.config = SceytChatUIKitConfig()
    }

    @Test
    fun `handlePush saves push and enqueues notification work with message payload`() = runTest {
        val workManager = installWorkManagerMock()
        val data = pushData(messageId = 42, channelId = 7)
        whenever(messagesLogic.handlePush(data)).thenReturn(true)
        setNotificationConfig(
            PushNotificationConfig(
                isPushEnabled = true,
                shouldDisplayNotification = { it == data }
            )
        )

        service(this).handlePush(data)
        advanceUntilIdle()

        verify(messagesLogic).handlePush(data)
        assertThat(workManager.requests).hasSize(1)
        val request = workManager.requests.single()
        assertThat(request.workSpec.workerClassName)
            .isEqualTo(HandleNotificationWorker::class.java.name)
        assertThat(request.tags).contains(SCEYT_WORKER_TAG)
        assertThat(request.workSpec.expedited).isTrue()
        assertThat(request.workSpec.outOfQuotaPolicy)
            .isEqualTo(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)

        val input = request.workSpec.input
        assertThat(input.getInt(HandleNotificationWorkManager.NOTIFICATION_TYPE, -1))
            .isEqualTo(NotificationType.ChannelMessage.ordinal)
        assertThat(input.getLong(HandleNotificationWorkManager.CHANNEL_ID, -1))
            .isEqualTo(7)
        assertThat(input.getLong(HandleNotificationWorkManager.MESSAGE_ID, -1))
            .isEqualTo(42)
        assertThat(input.getString(HandleNotificationWorkManager.USER_ID))
            .isEqualTo("sender")
        assertThat(input.keyValueMap)
            .doesNotContainKey(HandleNotificationWorkManager.REACTION_ID)
        verify(workManager.continuation).enqueue()
    }

    @Test
    fun `handlePush includes reaction id when notification data has reaction`() = runTest {
        val workManager = installWorkManagerMock()
        val reaction = reaction(id = 99, messageId = 43)
        val data = pushData(
            type = NotificationType.MessageReaction,
            messageId = 43,
            reaction = reaction
        )
        whenever(messagesLogic.handlePush(data)).thenReturn(true)
        setNotificationConfig(PushNotificationConfig(isPushEnabled = true))

        service(this).handlePush(data)
        advanceUntilIdle()

        val input = workManager.requests.single().workSpec.input
        assertThat(input.getInt(HandleNotificationWorkManager.NOTIFICATION_TYPE, -1))
            .isEqualTo(NotificationType.MessageReaction.ordinal)
        assertThat(input.getLong(HandleNotificationWorkManager.REACTION_ID, -1))
            .isEqualTo(99)
    }

    @Test
    fun `handlePush skips notification work when persistence rejects push`() = runTest {
        val workManager = installWorkManagerMock()
        val displayChecks = mutableListOf<PushData>()
        val data = pushData()
        whenever(messagesLogic.handlePush(data)).thenReturn(false)
        setNotificationConfig(
            PushNotificationConfig(
                isPushEnabled = true,
                shouldDisplayNotification = {
                    displayChecks += it
                    true
                }
            )
        )

        service(this).handlePush(data)
        advanceUntilIdle()

        assertThat(displayChecks).isEmpty()
        assertThat(workManager.requests).isEmpty()
        verifyNoInteractions(workManager.continuation)
    }

    @Test
    fun `handlePush skips notification work when push notifications are disabled`() = runTest {
        val workManager = installWorkManagerMock()
        val displayChecks = mutableListOf<PushData>()
        val data = pushData()
        whenever(messagesLogic.handlePush(data)).thenReturn(true)
        setNotificationConfig(
            PushNotificationConfig(
                isPushEnabled = false,
                shouldDisplayNotification = {
                    displayChecks += it
                    true
                }
            )
        )

        service(this).handlePush(data)
        advanceUntilIdle()

        assertThat(displayChecks).isEmpty()
        assertThat(workManager.requests).isEmpty()
        verifyNoInteractions(workManager.continuation)
    }

    @Test
    fun `handlePush skips notification work when display predicate returns false`() = runTest {
        val workManager = installWorkManagerMock()
        val data = pushData()
        whenever(messagesLogic.handlePush(data)).thenReturn(true)
        setNotificationConfig(
            PushNotificationConfig(
                isPushEnabled = true,
                shouldDisplayNotification = { false }
            )
        )

        service(this).handlePush(data)
        advanceUntilIdle()

        assertThat(workManager.requests).isEmpty()
        verifyNoInteractions(workManager.continuation)
    }

    @Test
    fun `registerPushDevice does not register when token and service are already subscribed`() {
        whenever(chatClient.pushSubscriptions).thenReturn(
            listOf(
                PushSubscription(
                    "client",
                    "token",
                    null,
                    PushServiceType.Fcm.stingValue(),
                    "android"
                )
            )
        )

        service().registerPushDevice(PushDevice("token", PushServiceType.Fcm))

        verify(chatClient, never()).registerPushToken(any(), any(), any())
    }

    @Test
    fun `registerPushDevice registers missing subscription and exposes sdk callbacks`() {
        val callbackCaptor = argumentCaptor<ActionCallback>()

        service().registerPushDevice(PushDevice("token", PushServiceType.Fcm))

        verify(chatClient).registerPushToken(
            eq("token"),
            eq(PushServiceType.Fcm.stingValue()),
            callbackCaptor.capture()
        )
        callbackCaptor.firstValue.onSuccess()
        callbackCaptor.firstValue.onError(SceytException(401, "denied"))
    }

    @Test
    fun `ensurePushTokenRegistered uses first supported push provider`() {
        val unsupportedProvider = mock<PushDeviceProvider>()
        val supportedProvider = mock<PushDeviceProvider>()
        val callbackCaptor = argumentCaptor<(PushDevice) -> Unit>()
        whenever(unsupportedProvider.isSupported(context)).thenReturn(false)
        whenever(supportedProvider.isSupported(context)).thenReturn(true)
        setNotificationConfig(
            PushNotificationConfig(
                pushProviders = listOf(unsupportedProvider, supportedProvider)
            )
        )

        service().ensurePushTokenRegistered()

        verify(unsupportedProvider).isSupported(context)
        verify(supportedProvider).isSupported(context)
        verify(unsupportedProvider, never()).generatePushDeviceAsync(any())
        verify(supportedProvider).generatePushDeviceAsync(callbackCaptor.capture())

        callbackCaptor.firstValue(PushDevice("generated", PushServiceType.Hms))

        verify(chatClient).registerPushToken(
            eq("generated"),
            eq(PushServiceType.Hms.stingValue()),
            any()
        )
    }

    @Test
    fun `ensurePushTokenRegistered does nothing when no provider is supported`() {
        val provider = mock<PushDeviceProvider>()
        whenever(provider.isSupported(context)).thenReturn(false)
        setNotificationConfig(PushNotificationConfig(pushProviders = listOf(provider)))

        service().ensurePushTokenRegistered()

        verify(provider).isSupported(context)
        verify(provider, never()).generatePushDeviceAsync(any())
        verify(chatClient, never()).registerPushToken(any(), any(), any())
    }

    @Test
    fun `unregisterPushDevice forwards success result`() {
        val callbackCaptor = argumentCaptor<ActionCallback>()
        val results = mutableListOf<Result<Boolean>>()

        service().unregisterPushDevice(results::add)

        verify(chatClient).unregisterPushToken(callbackCaptor.capture())
        callbackCaptor.firstValue.onSuccess()

        assertThat(results.single().getOrThrow()).isTrue()
    }

    @Test
    fun `unregisterPushDevice forwards sdk error`() {
        val callbackCaptor = argumentCaptor<ActionCallback>()
        val results = mutableListOf<Result<Boolean>>()
        val exception = SceytException(500, "failed")

        service().unregisterPushDevice(results::add)

        verify(chatClient).unregisterPushToken(callbackCaptor.capture())
        callbackCaptor.firstValue.onError(exception)

        assertThat(results.single().exceptionOrNull()).isSameInstanceAs(exception)
    }

    @Test
    fun `unregisterPushDevice forwards unknown error when sdk error is null`() {
        val callbackCaptor = argumentCaptor<ActionCallback>()
        val results = mutableListOf<Result<Boolean>>()

        service().unregisterPushDevice(results::add)

        verify(chatClient).unregisterPushToken(callbackCaptor.capture())
        callbackCaptor.firstValue.onError(null)

        assertThat(results.single().exceptionOrNull()).hasMessageThat()
            .isEqualTo("Unknown error")
    }

    @Test
    fun `unregisterPushDevice allows missing callback`() {
        val callbackCaptor = argumentCaptor<ActionCallback>()

        service().unregisterPushDevice(null)

        verify(chatClient).unregisterPushToken(callbackCaptor.capture())
        callbackCaptor.firstValue.onSuccess()
        callbackCaptor.firstValue.onError(SceytException(1, "ignored"))
    }

    private fun service(
        scope: TestScope = TestScope(StandardTestDispatcher()),
        messagesLogic: PersistenceMessagesLogic = this@PushServiceImplTest.messagesLogic,
    ) = PushServiceImpl(context, scope, messagesLogic)

    private fun setNotificationConfig(notificationConfig: PushNotificationConfig) {
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            this.notificationConfig = notificationConfig
        }
    }

    private fun pushData(
        type: NotificationType = NotificationType.ChannelMessage,
        channelId: Long = 1,
        messageId: Long = 2,
        reaction: SceytReaction? = null,
    ): PushData {
        val user = SceytUser("sender")
        val message = createMessage(createdAt = messageId, id = messageId, tid = messageId)
            .copy(channelId = channelId, user = user)
        val channel = createChannel(
            id = channelId,
            pinnedAt = 0,
            createdAt = 1,
            lastMessage = message
        )
        return PushData(
            type = type,
            channel = channel,
            message = message,
            user = user,
            reaction = reaction
        )
    }

    private fun reaction(id: Long, messageId: Long): SceytReaction {
        return SceytReaction(
            id = id,
            messageId = messageId,
            key = "like",
            score = 1,
            reason = "",
            createdAt = 1,
            user = SceytUser("reactor"),
            pending = false
        )
    }

    private fun installWorkManagerMock(): CapturingWorkManager {
        val workManager = mock<WorkManagerImpl>()
        val workContinuation = mock<WorkContinuation>()
        val operation = mock<Operation>()
        val capturedRequests = mutableListOf<OneTimeWorkRequest>()
        WorkManagerImpl.setDelegate(workManager)

        doAnswer { invocation ->
            capturedRequests += invocation.getArgument<OneTimeWorkRequest>(0)
            workContinuation
        }.whenever(workManager).beginWith(any<OneTimeWorkRequest>())
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedRequests += invocation.getArgument<List<OneTimeWorkRequest>>(0)
            workContinuation
        }.whenever(workManager).beginWith(any<List<OneTimeWorkRequest>>())
        whenever(workContinuation.enqueue()).thenReturn(operation)

        return CapturingWorkManager(
            workManager = workManager,
            continuation = workContinuation,
            requests = capturedRequests
        )
    }

    private data class CapturingWorkManager(
        val workManager: WorkManagerImpl,
        val continuation: WorkContinuation,
        val requests: List<OneTimeWorkRequest>,
    )
}
