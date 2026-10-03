package com.sceyt.chatuikit.notifications

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationHandler
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ShouldShowNotificationUseCase
import com.sceyt.chatuikit.push.PushData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PinnedNotificationPolicyTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val originalConfig = SceytChatUIKit.config
    private val originalChannelId = ChannelsCache.currentChannelId
    private val policy = ShouldShowNotificationUseCase(context)
    private val handler = TestNotificationHandler(context)

    @Before
    fun setUp() {
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            notificationConfig = PushNotificationConfig(suppressWhenAppIsInForeground = false)
        }
        ChannelsCache.currentChannelId = null
    }

    @After
    fun tearDown() {
        SceytChatUIKit.config = originalConfig
        ChannelsCache.currentChannelId = originalChannelId
    }

    @Test
    fun `receiving a message does not consume its later pin notification`() {
        val received = data(NotificationType.ChannelMessage)
        assertThat(handler.alreadyShown(received)).isFalse()

        val pin = received.copy(type = NotificationType.MessagePinned)
        assertThat(policy(pin)).isTrue()
        assertThat(handler.alreadyShown(pin)).isFalse()
    }

    @Test
    fun `a pin notification does not consume the original message notification`() {
        val pin = data(NotificationType.MessagePinned)
        assertThat(handler.alreadyShown(pin)).isFalse()
        assertThat(handler.alreadyShown(pin.copy(type = NotificationType.ChannelMessage))).isFalse()
    }

    @Test
    fun `another pin of the same message is a new notification event`() {
        val pin = data(NotificationType.MessagePinned)
        assertThat(handler.alreadyShown(pin)).isFalse()
        assertThat(handler.alreadyShown(pin)).isFalse()
    }

    @Test
    fun `someone else pinning my outgoing message is eligible for notification`() {
        assertThat(policy(data(NotificationType.MessagePinned, incoming = false))).isTrue()
    }

    @Test
    fun `ordinary outgoing messages remain suppressed`() {
        assertThat(policy(data(NotificationType.ChannelMessage, incoming = false))).isFalse()
    }

    @Test
    fun `ordinary message duplicates remain suppressed`() {
        val message = data(NotificationType.ChannelMessage)
        assertThat(handler.alreadyShown(message)).isFalse()
        assertThat(handler.alreadyShown(message)).isTrue()
    }

    @Test
    fun `muted channels still suppress pin notifications`() {
        val pin = data(NotificationType.MessagePinned)
        assertThat(policy(pin.copy(channel = pin.channel.copy(muted = true)))).isFalse()
    }

    private fun data(type: NotificationType, incoming: Boolean = true): PushData {
        val author = SceytUser(if (incoming) "other-author" else "me")
        return PushData(
            type = type,
            channel = createChannel(id = 7L, pinnedAt = 0L, createdAt = 1L),
            message = createMessage(createdAt = 100L, id = 42L, tid = 77L)
                .copy(channelId = 7L, incoming = incoming, user = author),
            user = if (type == NotificationType.MessagePinned) SceytUser("pinner") else author,
            reaction = null,
        )
    }

    private class TestNotificationHandler(context: Context) : DefaultPushNotificationHandler(context) {
        fun alreadyShown(data: PushData) = checkMaybeAlreadyShown(data)
    }
}
