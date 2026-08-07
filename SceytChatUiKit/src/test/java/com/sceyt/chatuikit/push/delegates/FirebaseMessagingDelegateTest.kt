package com.sceyt.chatuikit.push.delegates

import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.push.PushHandleResult
import com.sceyt.chatuikit.push.service.PushService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.reset
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseMessagingDelegateTest {

    @Before
    fun setUp() {
        stopKoin()
        reset(pushService)
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        SceytChatUIKit.config = SceytChatUIKitConfig()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<PushService> { pushService } })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
        SceytChatUIKit.config = SceytChatUIKitConfig()
    }

    @Test
    fun `handleRemoteMessage returns null and skips the service for a non chat push`() {
        val remoteMessage = remoteMessage(mapOf("app" to "other_app"))

        val data = FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)

        assertThat(data).isNull()
        verifyNoInteractions(pushService)
    }

    @Test
    fun `handleRemoteMessage returns null when the payload cannot be parsed`() {
        val remoteMessage = remoteMessage(
            chatPayload().toMutableMap().apply { remove("message") }
        )

        val data = FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)

        assertThat(data).isNull()
        verifyNoInteractions(pushService)
    }

    @Test
    fun `handleRemoteMessage parses the payload and delegates to the push service`() {
        val remoteMessage = remoteMessage(chatPayload())

        val data = FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)

        assertThat(data).isNotNull()
        requireNotNull(data)
        assertThat(data.type).isEqualTo(NotificationType.ChannelMessage)
        assertThat(data.channel.id).isEqualTo(CHANNEL_ID)
        assertThat(data.message.id).isEqualTo(MESSAGE_ID)
        assertThat(data.message.body).isEqualTo("hello")
        assertThat(data.user.id).isEqualTo(USER_ID)
        assertThat(data.reaction).isNull()
        verify(pushService).handlePush(eq(data), isNull())
    }

    @Test
    fun `handleRemoteMessage forwards the callback to the push service`() {
        val remoteMessage = remoteMessage(chatPayload())
        val results = mutableListOf<PushHandleResult>()
        val callbackCaptor = argumentCaptor<(PushHandleResult) -> Unit>()

        val data = FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage, results::add)

        requireNotNull(data)
        verify(pushService).handlePush(eq(data), callbackCaptor.capture())
        val result = PushHandleResult.Skipped(data)
        callbackCaptor.firstValue(result)

        assertThat(results.single()).isSameInstanceAs(result)
    }

    @Test
    fun `handleRemoteMessageSuspended returns the service result`() = runTest {
        val remoteMessage = remoteMessage(chatPayload())
        whenever(pushService.handlePushSuspended(org.mockito.kotlin.any())).thenAnswer {
            PushHandleResult.Handled(it.getArgument(0), notificationScheduled = true)
        }

        val result = FirebaseMessagingDelegate.handleRemoteMessageSuspended(remoteMessage)

        assertThat(result).isInstanceOf(PushHandleResult.Handled::class.java)
        assertThat(result?.data?.message?.id).isEqualTo(MESSAGE_ID)
    }

    @Test
    fun `handleRemoteMessageSuspended returns null for a non chat push`() = runTest {
        val remoteMessage = remoteMessage(mapOf("app" to "other_app"))

        val result = FirebaseMessagingDelegate.handleRemoteMessageSuspended(remoteMessage)

        assertThat(result).isNull()
        verifyNoInteractions(pushService)
    }

    private fun remoteMessage(payload: Map<String, String>): RemoteMessage {
        return mock<RemoteMessage>().also { whenever(it.data).thenReturn(payload) }
    }

    private fun chatPayload(): Map<String, String> = mapOf(
        "app" to "vt_chat",
        "type" to NotificationType.ChannelMessage.ordinal.toString(),
        "user" to """
            {"id":"$USER_ID","username":"sender","first_name":"Push","last_name":"Sender",
            "role":"member"}
        """.trimIndent(),
        "channel" to """
            {"id":"$CHANNEL_ID","type":"direct","uri":"","subject":"Chat","metadata":"",
            "members_count":2}
        """.trimIndent(),
        "message" to """
            {"id":"$MESSAGE_ID","body":"hello","type":"text","metadata":"",
            "created_at":"2026-08-07T10:00:00.000000Z","attachments":[]}
        """.trimIndent()
    )

    private companion object {
        private const val USER_ID = "sender"
        private const val CHANNEL_ID = 7L
        private const val MESSAGE_ID = 42L

        private val pushService = mock<PushService>()
    }
}