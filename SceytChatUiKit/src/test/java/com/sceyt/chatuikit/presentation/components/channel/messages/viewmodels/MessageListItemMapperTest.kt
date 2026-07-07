package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import org.junit.Test

class MessageListItemMapperTest {
    private val mapper = MessageListItemMapper()

    @Test
    fun `map adds date separator and loading items`() {
        val first = createMessage(createdAt = 1_000, id = 1, tid = 1)
        val second = createMessage(createdAt = 86_401_000, id = 2, tid = 2)

        val result = mapper.map(
            data = listOf(first, second),
            hasNext = true,
            hasPrev = true,
            enableDateSeparator = true,
            context = context()
        )

        assertThat(result[0]).isEqualTo(MessageListItem.LoadingPrevItem)
        assertThat(result[1]).isInstanceOf(DateSeparatorItem::class.java)
        assertThat(result[2]).isEqualTo(MessageItem(first))
        assertThat(result[3]).isInstanceOf(DateSeparatorItem::class.java)
        assertThat(result[4]).isEqualTo(MessageItem(second))
        assertThat(result[5]).isEqualTo(MessageListItem.LoadingNextItem)
    }

    @Test
    fun `map preserves selected and expanded message state`() {
        val message = createMessage(createdAt = 1_000, id = 1, tid = 10)

        val result = mapper.map(
            data = listOf(message),
            hasNext = false,
            hasPrev = false,
            enableDateSeparator = false,
            context = context(selected = setOf(10), expanded = setOf(10))
        )

        val item = result.single() as MessageItem
        assertThat(item.message.isSelected).isTrue()
        assertThat(item.message.isBodyExpanded).isTrue()
    }

    @Test
    fun `map inserts unread separator after pinned last read message`() {
        val pinnedMessage = createMessage(createdAt = 1_000, id = 1, tid = 1)
        val nextMessage = createMessage(createdAt = 2_000, id = 2, tid = 2)
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)
            .copy(lastMessage = createMessage(createdAt = 3_000, id = 3, tid = 3).copy(incoming = true))

        val result = mapper.map(
            data = listOf(nextMessage),
            hasNext = false,
            hasPrev = false,
            compareMessage = pinnedMessage,
            enableDateSeparator = false,
            context = context(channel = channel, pinnedLastReadMessageId = pinnedMessage.id)
        )

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(
            MessageListItem.UnreadMessagesSeparatorItem(
                createdAt = nextMessage.createdAt,
                msgId = pinnedMessage.id
            )
        )
        assertThat(result[1]).isEqualTo(MessageItem(nextMessage))
    }

    @Test
    fun `init message info sets group avatar state`() {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1).copy(type = "group")
        val message = createMessage(createdAt = 1_000, id = 1, tid = 1).copy(incoming = true)

        val result = mapper.initMessageInfoData(
            sceytMessage = message,
            initNameAndAvatar = true,
            context = context(channel = channel)
        )

        assertThat(result.isGroup).isTrue()
        assertThat(result.shouldShowAvatarAndName).isTrue()
        assertThat(result.disabledShowAvatarAndName).isFalse()
    }

    @Test
    fun `init message info disables avatar when setting is off`() {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1).copy(type = "group")
        val message = createMessage(createdAt = 1_000, id = 1, tid = 1).copy(incoming = true)

        val result = mapper.initMessageInfoData(
            sceytMessage = message,
            initNameAndAvatar = true,
            context = context(channel = channel, showSenderAvatarAndName = false)
        )

        assertThat(result.shouldShowAvatarAndName).isFalse()
        assertThat(result.disabledShowAvatarAndName).isTrue()
    }

    private fun context(
        channel: SceytChannel = createChannel(id = 1, pinnedAt = 0, createdAt = 1),
        myId: String? = null,
        pinnedLastReadMessageId: Long = 0,
        showSenderAvatarAndName: Boolean = true,
        selected: Set<Long> = emptySet(),
        expanded: Set<Long> = emptySet(),
    ) = MessageListItemMappingContext(
        channel = channel,
        myIdProvider = { myId },
        pinnedLastReadMessageId = pinnedLastReadMessageId,
        showSenderAvatarAndName = showSenderAvatarAndName,
        selectedMessageTids = selected,
        expandedMessageTids = expanded,
    )
}
