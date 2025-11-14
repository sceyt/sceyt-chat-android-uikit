package com.sceyt.chatuikit

import com.google.common.annotations.VisibleForTesting
import com.google.common.truth.Truth
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import org.junit.Test


class ChannelsComparatorDescByTest {

    @Test
    fun sort_channels_by_channel_created_at_without_pinned_at() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1, 0, 4))
        channels.add(createChannel(1, 0, 2))
        channels.add(createChannel(1, 0, 1))
        channels.add(createChannel(1, 0, 3))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderCreatedAt))

        Truth.assertThat(channels.map { it.createdAt }.also {
            println("channels: $it")
        } == listOf(4L, 3L, 2L, 1L)).isTrue()
    }


    @Test
    fun sort_channels_by_channel_created_at_with_pinned_at() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1, 1, 4))
        channels.add(createChannel(2, 0, 1))
        channels.add(createChannel(3, 2, 2))
        channels.add(createChannel(4, 0, 3))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderCreatedAt))

        Truth.assertThat(channels.map { it.id }.also {
            println("channels: $it")
        } == listOf(3L, 1L, 4L, 2L)).isTrue()
    }

    @Test
    fun sort_channels_by_last_message_createdAt() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1L, 0, 1, createMessage(4)))
        channels.add(createChannel(2L, 0, 2, createMessage(2)))
        channels.add(createChannel(3L, 0, 3, createMessage(1)))
        channels.add(createChannel(4L, 0, 4, createMessage(3)))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderLastMessage))

        Truth.assertThat(channels.map { it.id }.also {
            println("channels: $it")
        } == listOf(1L, 4L, 2L, 3L)).isTrue()
    }

    @Test
    fun sort_channels_by_last_message_createdAt_with_pinnedAt() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1L, 1, 1, createMessage(4)))
        channels.add(createChannel(2L, 0, 2, createMessage(2)))
        channels.add(createChannel(3L, 2, 3, createMessage(1)))
        channels.add(createChannel(4L, 0, 4, createMessage(3)))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderLastMessage))

        Truth.assertThat(channels.map { it.id }.also {
            println("channels: $it")
        } == listOf(3L, 1L, 4L, 2L)).isTrue()
    }

    @Test
    fun sort_channels_by_last_message_createdAt_and_channel_createdAt_when_lastMessage_id_null() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1, 0, 1, createMessage(4)))
        channels.add(createChannel(2, 0, 2))
        channels.add(createChannel(3, 0, 3, createMessage(1)))
        channels.add(createChannel(4, 0, 4))
        channels.add(createChannel(5, 0, 5, createMessage(3)))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderLastMessage))

        Truth.assertThat(channels.map { it.id }.also {
            println("channels: $it")
        } == listOf(1L, 5L, 3L, 4L, 2L)).isTrue()
    }

    @Test
    fun sort_channels_by_last_message_createdAt_and_channel_createdAt_when_lastMessage_id_null_with_pinned() {
        val channels = mutableListOf<SceytChannel>()
        channels.add(createChannel(1, 2, 1, createMessage(4)))
        channels.add(createChannel(2, 0, 2))
        channels.add(createChannel(3, 0, 3, createMessage(1)))
        channels.add(createChannel(4, 1, 4))
        channels.add(createChannel(5, 0, 5, createMessage(3)))

        channels.sortWith(ChannelsComparatorDescBy(ChannelListOrder.ListQueryChannelOrderLastMessage))

        Truth.assertThat(channels.map { it.id }.also {
            println("channels: $it")
        } == listOf(1L, 4L, 5L, 3L, 2L)).isTrue()
    }
}

@VisibleForTesting
fun createChannel(id: Long, pinnedAt: Long, createdAt: Long, lastMessage: SceytMessage? = null) = SceytChannel(
    id = id,
    parentChannelId = 0,
    uri = null,
    type = "direct",
    subject = "",
    avatarUrl = "",
    metadata = null,
    createdAt = createdAt,
    updatedAt = 0,
    messagesClearedAt = 0,
    memberCount = 1,
    createdBy = null,
    userRole = null,
    unread = false,
    newMessageCount = 0,
    newMentionCount = 0,
    newReactedMessageCount = 0,
    hidden = false,
    archived = false,
    muted = false,
    mutedTill = 0,
    pinnedAt = pinnedAt,
    lastReceivedMessageId = 0,
    lastDisplayedMessageId = 0,
    messageRetentionPeriod = 0,
    lastMessage = lastMessage,
    messages = null,
    members = null,
    newReactions = null,
    pendingReactions = null,
    pending = false,
    draftMessage = null,
    events = null
)

private fun createMessage(createdAt: Long): SceytMessage {
    return SceytMessage(
        id = 0,
        tid = 0,
        0,
        "",
        "",
        null,
        createdAt,
        incoming = false,
        isTransient = false,
        silent = false,
        deliveryStatus = DeliveryStatus.Displayed,
        state = MessageState.Unmodified,
        replyCount = 0L,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetails = null,
        bodyAttributes = null,
        attachments = null,
        markerTotals = null,
        reactionTotals = null,
        parentMessage = null,
        pendingReactions = null,
        mentionedUsers = null,
        userReactions = null,
        updatedAt = 0,
        user = null,
        userMarkers = null,
        disableMentionsCount = false,
        poll = null,
    )
}