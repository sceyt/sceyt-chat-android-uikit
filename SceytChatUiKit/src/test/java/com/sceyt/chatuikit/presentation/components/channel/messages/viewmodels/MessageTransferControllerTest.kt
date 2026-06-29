package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageTransferControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val fileTransferService = mock<FileTransferService>()
    private val pauseOrResumeTransferUseCase = mock<PauseOrResumeTransferUseCase>()
    private val channelId = 7L

    private fun CoroutineScope.controller() = MessageTransferController(
        scope = this,
        defaultDispatcher = dispatcher,
        mainDispatcher = dispatcher,
        fileTransferService = fileTransferService,
        pauseOrResumeTransferUseCase = pauseOrResumeTransferUseCase,
        store = MessageListStore(),
        channelId = { channelId },
        ioDispatcher = dispatcher,
    )

    private fun transfer(state: TransferState, thumbKey: Int? = null) = TransferData(
        messageTid = 1,
        progressPercent = 0f,
        state = state,
        filePath = null,
        url = null,
        thumbData = thumbKey?.let { keyValue -> mock<ThumbData> { on { key } doReturn keyValue } }
    )

    private fun attachment(
        messageTid: Long = 1,
        url: String = "https://example.com/file.jpg",
        filePath: String? = null,
        transferState: TransferState? = null,
    ) = SceytAttachment(
        id = 1,
        messageId = 1,
        messageTid = messageTid,
        userId = null,
        name = "file.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100,
        createdAt = 1,
        url = url,
        filePath = filePath,
        transferState = transferState,
        progressPercent = null,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun firstMessageAttachment(store: MessageListStore): SceytAttachment {
        return store.items.filterIsInstance<MessageItem>().single().message.attachments!!.single()
    }

    @Test
    fun `isMessageListThumbLoaded only for ThumbLoaded with the messages-list key`() = runTest(dispatcher) {
        val controller = controller()

        assertThat(controller.isMessageListThumbLoaded(transfer(TransferState.ThumbLoaded, ThumbFor.MessagesLisView.value))).isTrue()
        assertThat(controller.isMessageListThumbLoaded(transfer(TransferState.ThumbLoaded, thumbKey = 999))).isFalse()
        assertThat(controller.isMessageListThumbLoaded(transfer(TransferState.Uploading))).isFalse()
    }

    @Test
    fun `shouldDeferTransferUpdate defers terminal-ish states, not active ones`() = runTest(dispatcher) {
        val controller = controller()

        assertThat(controller.shouldDeferTransferUpdate(transfer(TransferState.FilePathChanged))).isTrue()
        assertThat(controller.shouldDeferTransferUpdate(transfer(TransferState.ThumbLoaded, ThumbFor.MessagesLisView.value))).isTrue()
        assertThat(controller.shouldDeferTransferUpdate(transfer(TransferState.Uploading))).isFalse()
    }

    @Test
    fun `needMediaInfo NeedDownload triggers a download`() = runTest(dispatcher) {
        whenever(fileTransferService.findOrCreateTransferTask(any())).thenReturn(mock())
        val controller = controller()

        controller.needMediaInfo(NeedMediaInfoData.NeedDownload(mock<SceytAttachment>()))
        advanceUntilIdle()

        verifyBlocking(fileTransferService) { download(any(), any()) }
    }

    @Test
    fun `needMediaInfo NeedThumb triggers a thumb fetch`() = runTest(dispatcher) {
        val controller = controller()

        controller.needMediaInfo(NeedMediaInfoData.NeedThumb(mock<SceytAttachment>(), mock<ThumbData>()))
        advanceUntilIdle()

        verifyBlocking(fileTransferService) { getThumb(any(), any(), any()) }
    }

    @Test
    fun `flushDeferred is a no-op when nothing was parked`() = runTest(dispatcher) {
        val controller = controller()

        controller.flushDeferred()
        advanceUntilIdle()

        // No store/message present; just assert it doesn't blow up and touches no message lookup state.
        verifyBlocking(fileTransferService, org.mockito.kotlin.never()) { findOrCreateTransferTask(any()) }
    }

    @Test
    fun `deferred updates are applied and cleared on flush`() = runTest(dispatcher) {
        val store = MessageListStore().apply { enableDateSeparator = false }
        val url = "https://example.com/file.jpg"
        store.replace(
            items = listOf(
                MessageItem(
                    createMessage(createdAt = 1, id = 1, tid = 1)
                        .copy(attachments = listOf(attachment(url = url)))
                )
            ),
            force = false
        )
        val controller = MessageTransferController(
            scope = this,
            defaultDispatcher = dispatcher,
            mainDispatcher = dispatcher,
            fileTransferService = fileTransferService,
            pauseOrResumeTransferUseCase = pauseOrResumeTransferUseCase,
            store = store,
            channelId = { channelId },
            ioDispatcher = dispatcher,
        )

        controller.deferUpdate(
            TransferData(
                messageTid = 1,
                progressPercent = 100f,
                state = TransferState.Downloaded,
                filePath = "/local/file.jpg",
                url = url,
            )
        )
        controller.flushDeferred()
        advanceUntilIdle()

        val revisionAfterFirstFlush = store.state.value.revision
        val attachmentAfterFirstFlush = firstMessageAttachment(store)
        assertThat(attachmentAfterFirstFlush.filePath).isEqualTo("/local/file.jpg")
        assertThat(attachmentAfterFirstFlush.transferState).isEqualTo(TransferState.Downloaded)
        assertThat(attachmentAfterFirstFlush.progressPercent).isEqualTo(100f)

        controller.flushDeferred()
        advanceUntilIdle()

        assertThat(store.state.value.revision).isEqualTo(revisionAfterFirstFlush)
        assertThat(firstMessageAttachment(store)).isEqualTo(attachmentAfterFirstFlush)
    }

    @Test
    fun `pauseOrResumeUpload invokes the use case with the channel id`() = runTest(dispatcher) {
        val attachment = mock<SceytAttachment>()
        val item = mock<FileListItem> { on { this.attachment } doReturn attachment }
        val controller = controller()

        controller.pauseOrResumeUpload(item)
        advanceUntilIdle()

        verifyBlocking(pauseOrResumeTransferUseCase) { invoke(eq(attachment), eq(channelId)) }
    }
}
