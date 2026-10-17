package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UnreadMentionsControllerTest {
    private val myId = "me"
    private val messageInteractor = mock<MessageInteractor>()
    private val channelInteractor = mock<ChannelInteractor>()
    private var channel: SceytChannel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)
    private val scrolledTo = mutableListOf<Long>()

    private fun CoroutineScope.controller() = UnreadMentionsController(
        scope = this,
        messageInteractor = messageInteractor,
        channelInteractor = channelInteractor,
        currentChannel = { channel },
        conversationId = { channel.id },
        updateChannel = { action -> channel = channel.action() },
        onScrollToMention = { scrolledTo.add(it) },
        currentUserId = { myId },
    )

    private fun mention(id: Long, mentionsMe: Boolean = true) =
        createMessage(createdAt = id, id = id, tid = id).copy(
            incoming = true,
            displayCount = 1,
            mentionedUsers = if (mentionsMe) listOf(createEmptyUser(myId)) else emptyList()
        )

    @Test
    fun `new mention message adds id and bumps count once`() = runTest {
        val controller = controller()

        controller.onNewMessage(mention(id = 1))
        controller.onNewMessage(mention(id = 1).copy(body = "edited"))

        assertThat(channel.newMentionCount).isEqualTo(1)
    }

    @Test
    fun `distinct mention messages bump count per id`() = runTest {
        val controller = controller()

        controller.onNewMessage(mention(id = 1))
        controller.onNewMessage(mention(id = 2))

        assertThat(channel.newMentionCount).isEqualTo(2)
    }

    @Test
    fun `non mention or non incoming message is ignored`() = runTest {
        val controller = controller()

        controller.onNewMessage(mention(id = 1, mentionsMe = false))
        controller.onNewMessage(mention(id = 2).copy(incoming = false))
        controller.onNewMessage(mention(id = 3).copy(displayCount = 0))
        controller.onNewMessage(mention(id = 4).copy(disableMentionsCount = true))

        assertThat(channel.newMentionCount).isEqualTo(0)
    }

    @Test
    fun `scroll to next emits the first tracked mention then the rest`() = runTest {
        val controller = controller()
        controller.onNewMessage(mention(id = 5))
        controller.onNewMessage(mention(id = 7))

        controller.prepareToScrollToNext()
        controller.prepareToScrollToNext()

        assertThat(scrolledTo).containsExactly(5L, 7L).inOrder()
    }

    @Test
    fun `read mentions are removed and no longer scrolled to`() = runTest {
        val controller = controller()
        controller.onNewMessage(mention(id = 5))
        controller.onNewMessage(mention(id = 7))

        controller.removeReadMentions(listOf(5L))
        controller.prepareToScrollToNext()

        assertThat(scrolledTo).containsExactly(7L)
    }

    @Test
    fun `message update that adds a mention syncs the channel from server`() = runTest {
        val controller = controller()

        controller.onMessageUpdated(mention(id = 1))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { getChannelFromServer(channel.id) }
    }

    @Test
    fun `message update without membership change does not hit server`() = runTest {
        val controller = controller()

        // id 1 not tracked and does not mention me -> no membership change.
        controller.onMessageUpdated(mention(id = 1, mentionsMe = false))
        advanceUntilIdle()

        verifyNoInteractions(channelInteractor)
    }

    @Test
    fun `onInit loads unread mentions when channel has a mention count`() = runTest {
        channel = channel.copy(newMentionCount = 2)
        whenever { messageInteractor.getUnreadMentions(any(), any(), any(), any()) }
            .thenReturn(SceytPagingResponse.Success(listOf(10L, 20L), hasNext = false))
        val controller = controller()

        controller.onInit()
        advanceUntilIdle()
        controller.prepareToScrollToNext()

        assertThat(scrolledTo).containsExactly(10L)
    }

    @Test
    fun `empty load result resets the channel mention count`() = runTest {
        channel = channel.copy(newMentionCount = 3)
        whenever { messageInteractor.getUnreadMentions(any(), any(), any(), any()) }
            .thenReturn(SceytPagingResponse.Success(emptyList(), hasNext = false))
        val controller = controller()

        controller.onInit()
        advanceUntilIdle()

        assertThat(channel.newMentionCount).isEqualTo(0)
    }
}