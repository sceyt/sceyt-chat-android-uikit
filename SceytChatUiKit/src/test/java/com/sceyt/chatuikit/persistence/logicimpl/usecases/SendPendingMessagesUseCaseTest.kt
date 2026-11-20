package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ForwardingDetailsDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.message.PendingSendRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SendPendingMessagesUseCaseTest {

    private val channelId = 7L
    private val dispatcher = StandardTestDispatcher()
    private val messageDao = mock<MessageDao>()
    private val registry = PendingSendRegistry()
    private val pendingRows = linkedMapOf<Long, MessageDb>()
    private var connected = true
    private val sent = mutableListOf<Long>()

    private val useCase = SendPendingMessagesUseCase(
        messageDao = messageDao,
        registry = registry,
        isConnected = { connected },
        ioDispatcher = dispatcher
    )

    private suspend fun givenPending(vararg rows: MessageDb) {
        rows.forEach { pendingRows[it.messageEntity.tid] = it }
        whenever(messageDao.getPendingMessages(channelId)).thenAnswer { pendingRows.values.toList() }
        whenever(messageDao.getPendingMessageByTid(any())).thenAnswer { pendingRows[it.getArgument(0)] }
    }

    private fun recordingSender(onSend: suspend (MessageDb) -> Unit = {}) =
        PendingMessageSender { message, _ ->
            sent += message.messageEntity.tid
            onSend(message)
        }

    private fun markSent(message: MessageDb) {
        pendingRows.remove(message.messageEntity.tid)
    }

    @Test
    fun `sends every pending message once`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L), pendingMessage(tid = 2L))

        val hadPending = useCase(channelId, skipPausedUploads = true, sender = recordingSender(::markSent))

        assertThat(hadPending).isTrue()
        assertThat(sent).containsExactly(1L, 2L).inOrder()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a message being sent is skipped by a concurrent call`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L))
        val firstSendGate = CompletableDeferred<Unit>()
        val sender = recordingSender { firstSendGate.await() }

        val first = launch { useCase(channelId, skipPausedUploads = true, sender = sender) }
        advanceUntilIdle()
        useCase(channelId, skipPausedUploads = true, sender = sender)
        firstSendGate.complete(Unit)
        first.join()

        assertThat(sent).containsExactly(1L)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a message sent by an earlier call is not resent from a stale list`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L), pendingMessage(tid = 2L))
        val firstSendGate = CompletableDeferred<Unit>()
        val sender = recordingSender { message ->
            if (message.messageEntity.tid == 1L) firstSendGate.await()
            markSent(message)
        }

        val first = launch { useCase(channelId, skipPausedUploads = true, sender = sender) }
        advanceUntilIdle()
        useCase(channelId, skipPausedUploads = true, sender = sender)
        firstSendGate.complete(Unit)
        first.join()

        assertThat(sent).containsExactly(1L, 2L)
    }

    @Test
    fun `nothing is sent while disconnected`() = runTest(dispatcher) {
        connected = false
        givenPending(pendingMessage(tid = 1L))

        val hadPending = useCase(channelId, skipPausedUploads = true, sender = recordingSender())

        assertThat(hadPending).isFalse()
        assertThat(sent).isEmpty()
        verify(messageDao, never()).getPendingMessages(any())
    }

    @Test
    fun `returns false when the channel has no pending messages`() = runTest(dispatcher) {
        givenPending()

        val hadPending = useCase(channelId, skipPausedUploads = true, sender = recordingSender())

        assertThat(hadPending).isFalse()
        assertThat(sent).isEmpty()
    }

    @Test
    fun `a message whose upload is paused is skipped`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L, attachmentState = TransferState.PauseUpload))

        useCase(channelId, skipPausedUploads = true, sender = recordingSender())

        assertThat(sent).isEmpty()
    }

    @Test
    fun `a paused upload is sent when skipping is off`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L, attachmentState = TransferState.PauseUpload))

        useCase(channelId, skipPausedUploads = false, sender = recordingSender())

        assertThat(sent).containsExactly(1L)
    }

    @Test
    fun `a failed send releases the message for the next attempt`() = runTest(dispatcher) {
        givenPending(pendingMessage(tid = 1L))
        val failingSender = PendingMessageSender { _, _ -> error("network") }

        runCatching { useCase(channelId, skipPausedUploads = true, sender = failingSender) }
        useCase(channelId, skipPausedUploads = true, sender = recordingSender())

        assertThat(sent).containsExactly(1L)
    }

    @Test
    fun `a forwarded message with uploaded attachments is flagged as uploaded`() = runTest(dispatcher) {
        givenPending(
            pendingMessage(tid = 1L, attachmentState = TransferState.Uploaded, forwarded = true),
            pendingMessage(tid = 2L, attachmentState = TransferState.Uploaded),
            pendingMessage(tid = 3L, attachmentState = TransferState.WaitingToUpload, forwarded = true),
        )
        val flags = mutableMapOf<Long, Boolean>()

        useCase(channelId, skipPausedUploads = true) { message, isUploadedForward ->
            flags[message.messageEntity.tid] = isUploadedForward
        }

        assertThat(flags).containsExactly(1L, true, 2L, false, 3L, false)
    }

    private fun pendingMessage(
        tid: Long,
        attachmentState: TransferState? = null,
        forwarded: Boolean = false,
    ): MessageDb {
        val entity = messageEntity(
            id = 0L,
            tid = tid,
            channelId = channelId,
            deliveryStatus = MessageDeliveryStatus.Pending
        ).copy(forwardingDetailsDb = if (forwarded) ForwardingDetailsDb(1L, "user", 1) else null)
        val attachments = attachmentState?.let { listOf(fileAttachment(tid, it)) }
        return messageDb(entity = entity).copy(attachments = attachments)
    }

    private fun fileAttachment(tid: Long, state: TransferState) = AttachmentDb(
        attachmentEntity = AttachmentEntity(
            id = null,
            messageId = 0L,
            messageTid = tid,
            channelId = channelId,
            userId = null,
            name = "file",
            type = "file",
            metadata = null,
            fileSize = 0L,
            createdAt = 0L,
            url = null,
            filePath = null,
            originalFilePath = null,
            viewOnce = false
        ),
        payLoad = AttachmentPayLoadEntity(
            messageTid = tid,
            transferState = state,
            progressPercent = null,
            url = null,
            filePath = null
        ),
        linkDetails = null
    )
}
