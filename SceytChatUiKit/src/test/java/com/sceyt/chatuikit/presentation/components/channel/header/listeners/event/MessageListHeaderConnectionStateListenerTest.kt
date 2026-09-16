package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.ConnectionStateListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.EventListeners
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageListHeaderConnectionStateListenerTest {
    private val channel = createChannel(1L, 0L, 0L)
    private val calls = mutableListOf<String>()
    private val defaultListeners = object : EventListeners, ConnectionStateListener {
        override fun onActivityEvent(event: ChannelMemberActivityEvent) = Unit

        override fun onPresenceUpdateEvent(channel: SceytChannel) {
            calls += "default presence"
        }

        override fun onConnectionStateChanged(state: ConnectionState, channel: SceytChannel) {
            calls += "default $state ${channel.id}"
        }
    }

    @Test
    fun `default presentation runs immediately before registered observers`() {
        val listener = MessageListHeaderEventsListenerImpl().withDefaultListeners(defaultListeners)
        listener.setListener(ConnectionStateListener { state, channel ->
            calls += "observer $state ${channel.id}"
        })

        listener.onConnectionStateChanged(ConnectionState.Disconnected, channel)

        assertEquals(listOf("default Disconnected 1", "observer Disconnected 1"), calls)
    }

    @Test
    fun `custom listener can suppress the default presentation`() {
        val listener = object : MessageListHeaderEventsListenerImpl() {
            override fun onConnectionStateChanged(state: ConnectionState, channel: SceytChannel) = Unit
        }
        listener.withDefaultListeners(defaultListeners)

        listener.onConnectionStateChanged(ConnectionState.Disconnected, channel)

        assertTrue(calls.isEmpty())
    }

    @Test
    fun `custom listener can defer forwarding to the default presentation`() {
        val listener = object : MessageListHeaderEventsListenerImpl() {
            private var pending: Pair<ConnectionState, SceytChannel>? = null

            override fun onConnectionStateChanged(state: ConnectionState, channel: SceytChannel) {
                pending = state to channel
            }

            fun forwardPendingState() {
                val (state, channel) = requireNotNull(pending)
                pending = null
                super.onConnectionStateChanged(state, channel)
            }
        }
        listener.withDefaultListeners(defaultListeners)

        listener.onConnectionStateChanged(ConnectionState.Disconnected, channel)
        assertTrue(calls.isEmpty())

        listener.forwardPendingState()
        assertEquals(listOf("default Disconnected 1"), calls)
    }

    @Test
    fun `registering a combined listener includes its connection callback`() {
        val listener = MessageListHeaderEventsListenerImpl().withDefaultListeners(defaultListeners)
        listener.setListener(object : MessageListHeaderEventsListenerImpl() {
            override fun onConnectionStateChanged(state: ConnectionState, channel: SceytChannel) {
                calls += "combined $state ${channel.id}"
            }
        })

        listener.onConnectionStateChanged(ConnectionState.Connecting, channel)

        assertEquals(listOf("default Connecting 1", "combined Connecting 1"), calls)
    }

    @Test
    fun `legacy listeners retain connection presentation and presence callbacks`() {
        val listener = MessageListHeaderEventsListenerImpl().withDefaultListeners(defaultListeners)
        listener.setListener(object : EventListeners {
            override fun onActivityEvent(event: ChannelMemberActivityEvent) = Unit

            override fun onPresenceUpdateEvent(channel: SceytChannel) {
                calls += "legacy presence"
            }

            override fun onConnectionStateChanged(
                state: ConnectionState,
                channel: SceytChannel
            ) = Unit
        })

        listener.onConnectionStateChanged(ConnectionState.Disconnected, channel)
        listener.onPresenceUpdateEvent(channel)

        assertEquals(listOf("default Disconnected 1", "default presence", "legacy presence"), calls)
    }
}
