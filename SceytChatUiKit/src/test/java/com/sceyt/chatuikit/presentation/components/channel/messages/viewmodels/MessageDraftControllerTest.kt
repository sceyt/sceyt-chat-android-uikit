package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageDraftControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val channelInteractor = mock<ChannelInteractor>()
    private val conversationId = 42L

    private var viewOnceSelected = false
    private val viewOnceSetters = mutableListOf<Boolean>()

    private fun CoroutineScope.controller() = MessageDraftController(
        scope = this,
        ioDispatcher = dispatcher,
        channelInteractor = channelInteractor,
        conversationId = { conversationId },
        isViewOnceSelected = { viewOnceSelected },
        setViewOnceSelected = {
            viewOnceSelected = it
            viewOnceSetters.add(it)
        },
    )

    private fun attachment(filePath: String?, type: String): Attachment = mock {
        whenever(it.filePath).thenReturn(filePath)
        whenever(it.type).thenReturn(type)
    }

    private fun CoroutineScope.update(
        attachments: List<Attachment> = emptyList(),
        mentionUsers: List<Mention> = emptyList(),
        isReply: Boolean = false,
    ) = controller().updateDraftMessage(
        text = null,
        attachments = attachments,
        audioRecordData = null,
        mentionUsers = mentionUsers,
        styling = null,
        replyOrEditMessage = null,
        isReply = isReply,
    )

    @Test
    fun `forwards draft message with channel id, null body and reply flag`() = runTest(dispatcher) {
        update(isReply = true)
        advanceUntilIdle()

        val captor = argumentCaptor<com.sceyt.chatuikit.data.models.channels.DraftMessage>()
        verifyBlocking(channelInteractor) { updateDraftMessage(captor.capture()) }
        val draft = captor.firstValue
        assertThat(draft.channelId).isEqualTo(conversationId)
        assertThat(draft.body).isNull()
        assertThat(draft.isReply).isTrue()
        assertThat(draft.viewOnce).isFalse()
    }

    @Test
    fun `maps mentions to body attributes and empty users`() = runTest(dispatcher) {
        update(mentionUsers = listOf(Mention(recipientId = "u1", name = "Alice", start = 0, length = 5)))
        advanceUntilIdle()

        val captor = argumentCaptor<com.sceyt.chatuikit.data.models.channels.DraftMessage>()
        verifyBlocking(channelInteractor) { updateDraftMessage(captor.capture()) }
        val draft = captor.firstValue
        assertThat(draft.mentionUsers).hasSize(1)
        assertThat(draft.mentionUsers?.single()?.id).isEqualTo("u1")
        assertThat(draft.bodyAttributes).hasSize(1)
    }

    @Test
    fun `skips attachments without file path and maps valid ones`() = runTest(dispatcher) {
        val valid = attachment(filePath = "/a.jpg", type = AttachmentTypeEnum.Image.value)
        val invalid = attachment(filePath = null, type = AttachmentTypeEnum.Image.value)
        update(attachments = listOf(valid, invalid))
        advanceUntilIdle()

        val captor = argumentCaptor<com.sceyt.chatuikit.data.models.channels.DraftMessage>()
        verifyBlocking(channelInteractor) { updateDraftMessage(captor.capture()) }
        val draft = captor.firstValue
        assertThat(draft.attachments).hasSize(1)
        assertThat(draft.attachments?.single()?.type).isEqualTo(AttachmentTypeEnum.Image)
    }

    @Test
    fun `view once cleared when attachment count not one`() = runTest(dispatcher) {
        viewOnceSelected = true
        val two = listOf(
            attachment(filePath = "/a.jpg", type = AttachmentTypeEnum.Image.value),
            attachment(filePath = "/b.jpg", type = AttachmentTypeEnum.Image.value),
        )
        update(attachments = two)
        advanceUntilIdle()

        assertThat(viewOnceSetters).containsExactly(false)
        val captor = argumentCaptor<com.sceyt.chatuikit.data.models.channels.DraftMessage>()
        verifyBlocking(channelInteractor) { updateDraftMessage(captor.capture()) }
        assertThat(captor.firstValue.viewOnce).isFalse()
    }

    @Test
    fun `view once kept when exactly one attachment`() = runTest(dispatcher) {
        viewOnceSelected = true
        update(attachments = listOf(attachment(filePath = "/a.jpg", type = AttachmentTypeEnum.Image.value)))
        advanceUntilIdle()

        assertThat(viewOnceSetters).isEmpty()
        val captor = argumentCaptor<com.sceyt.chatuikit.data.models.channels.DraftMessage>()
        verifyBlocking(channelInteractor) { updateDraftMessage(captor.capture()) }
        assertThat(captor.firstValue.viewOnce).isTrue()
    }
}
