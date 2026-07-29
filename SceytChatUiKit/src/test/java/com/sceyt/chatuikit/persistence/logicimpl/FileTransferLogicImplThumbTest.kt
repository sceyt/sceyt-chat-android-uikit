package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbCallback
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class FileTransferLogicImplThumbTest {
    private val fileTransferService = mock<FileTransferService>()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { fileTransferService }
            })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `thumb request for another target is not suppressed while message list thumb is preparing`() {
        val resolver = BlockingThumbPathResolver()
        val logic = FileTransferLogicImpl(context, attachmentLogic, resolver)
        val attachment = attachment()
        val callbacks = thumbCallbacksFor(attachment)
        val messageListThumb = thumbData(ThumbFor.MessagesLisView)
        val channelInfoThumb = thumbData(ThumbFor.ChannelInfo)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit {
                logic.getAttachmentThumb(attachment.messageTid, attachment, messageListThumb)
            }
            assertThat(resolver.awaitCallCount(1)).isTrue()

            val second = executor.submit {
                logic.getAttachmentThumb(attachment.messageTid, attachment, channelInfoThumb)
            }
            assertThat(resolver.awaitCallCount(2)).isTrue()

            resolver.release()
            first.get(1, TimeUnit.SECONDS)
            second.get(1, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        assertThat(resolver.callCount.get()).isEqualTo(2)
        assertThat(callbacks.map { it.key }).containsExactly(
            ThumbFor.MessagesLisView.value,
            ThumbFor.ChannelInfo.value
        )
    }

    @Test
    fun `duplicate thumb request for same target is suppressed while preparing`() {
        val resolver = BlockingThumbPathResolver()
        val logic = FileTransferLogicImpl(context, attachmentLogic, resolver)
        val attachment = attachment()
        val callbacks = thumbCallbacksFor(attachment)
        val channelInfoThumb = thumbData(ThumbFor.ChannelInfo)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit {
                logic.getAttachmentThumb(attachment.messageTid, attachment, channelInfoThumb)
            }
            assertThat(resolver.awaitCallCount(1)).isTrue()

            val second = executor.submit {
                logic.getAttachmentThumb(attachment.messageTid, attachment, channelInfoThumb)
            }
            second.get(1, TimeUnit.SECONDS)

            resolver.release()
            first.get(1, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        assertThat(resolver.callCount.get()).isEqualTo(1)
        assertThat(callbacks.map { it.key }).containsExactly(ThumbFor.ChannelInfo.value)
    }

    @Test
    fun `thumb request after file path changes reuses preparing original thumb and emits latest path`() {
        val resolver = BlockingThumbPathResolver()
        val logic = FileTransferLogicImpl(context, attachmentLogic, resolver)
        val originalAttachment = attachment(
            filePath = "/uploads/original.jpg",
            originalFilePath = "/uploads/original.jpg",
            url = null,
            state = TransferState.PendingUpload,
        )
        val resizedAttachment = originalAttachment.copy(filePath = "/uploads/resized.jpg")
        val callbacks = thumbCallbacksFor(originalAttachment)
        val originalThumb = thumbData(ThumbFor.MessagesLisView, "/uploads/original.jpg")
        val resizedThumb = thumbData(ThumbFor.MessagesLisView, "/uploads/resized.jpg")
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit {
                logic.getAttachmentThumb(originalAttachment.messageTid, originalAttachment, originalThumb)
            }
            assertThat(resolver.awaitCallCount(1)).isTrue()

            val second = executor.submit {
                logic.getAttachmentThumb(resizedAttachment.messageTid, resizedAttachment, resizedThumb)
            }
            second.get(1, TimeUnit.SECONDS)

            resolver.release()
            first.get(1, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        assertThat(resolver.callCount.get()).isEqualTo(1)
        assertThat(callbacks.map { it.filePath }).containsExactly("/uploads/resized.jpg")
    }

    @Test
    fun `thumb cache is reused for different messages with same source file`() {
        val resolver = BlockingThumbPathResolver().apply { release() }
        val logic = FileTransferLogicImpl(context, attachmentLogic, resolver)
        val firstAttachment = attachment(messageTid = 11L, filePath = "/downloads/shared.jpg")
        val secondAttachment = attachment(messageTid = 22L, filePath = "/downloads/shared.jpg")
        val callbacks = thumbCallbacksByTidFor(firstAttachment, secondAttachment)
        val thumb = thumbData(ThumbFor.MessagesLisView, "/downloads/shared.jpg")

        logic.getAttachmentThumb(firstAttachment.messageTid, firstAttachment, thumb)
        logic.getAttachmentThumb(secondAttachment.messageTid, secondAttachment, thumb)

        assertThat(resolver.callCount.get()).isEqualTo(1)
        assertThat(callbacks.getValue(firstAttachment.messageTid).map { it.filePath })
            .containsExactly("/downloads/shared.jpg")
        assertThat(callbacks.getValue(secondAttachment.messageTid).map { it.filePath })
            .containsExactly("/downloads/shared.jpg")
    }

    private fun thumbCallbacksFor(attachment: SceytAttachment): CopyOnWriteArrayList<ThumbData> {
        return thumbCallbacksByTidFor(attachment).getValue(attachment.messageTid)
    }

    private fun thumbCallbacksByTidFor(
        vararg attachments: SceytAttachment
    ): Map<Long, CopyOnWriteArrayList<ThumbData>> {
        val callbacksByTid = mutableMapOf<Long, CopyOnWriteArrayList<ThumbData>>()
        val tasksByTid = attachments.associate { attachment ->
            val callbacks = CopyOnWriteArrayList<ThumbData>()
            callbacksByTid[attachment.messageTid] = callbacks
            val task = TransferTask(attachment, attachment.messageTid, attachment.transferState)
            task.thumbCallback = ThumbCallback { _, thumbData -> callbacks.add(thumbData) }
            attachment.messageTid to task
        }

        whenever(fileTransferService.findOrCreateTransferTask(any())).thenAnswer { invocation ->
            val attachment = invocation.arguments.first() as SceytAttachment
            tasksByTid.getValue(attachment.messageTid)
        }

        return callbacksByTid
    }

    private class BlockingThumbPathResolver : ThumbPathResolver {
        val callCount = AtomicInteger()
        private val release = CountDownLatch(1)

        override fun getThumbPath(
            context: Context,
            attachment: SceytAttachment,
            size: Size,
        ): Result<String> {
            val callIndex = callCount.incrementAndGet()
            check(release.await(2, TimeUnit.SECONDS))
            return Result.success("/thumbs/thumb-$callIndex.jpg")
        }

        fun awaitCallCount(expectedCount: Int): Boolean {
            repeat(100) {
                if (callCount.get() >= expectedCount) return true
                Thread.sleep(10)
            }
            return false
        }

        fun release() {
            release.countDown()
        }
    }

    private fun thumbData(
        thumbFor: ThumbFor,
        filePath: String? = "/downloads/image.jpg",
    ) = ThumbData(
        key = thumbFor.value,
        filePath = filePath,
        size = Size(120, 120)
    )

    private fun attachment(
        messageTid: Long = MESSAGE_TID,
        filePath: String? = "/downloads/image.jpg",
        originalFilePath: String? = null,
        url: String? = "https://cdn.test/image.jpg",
        state: TransferState? = TransferState.Downloaded,
    ) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = url,
        filePath = filePath,
        transferState = state,
        progressPercent = 100f,
        originalFilePath = originalFilePath,
        linkPreviewDetails = null,
    )

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
