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

    private fun thumbCallbacksFor(attachment: SceytAttachment): CopyOnWriteArrayList<ThumbData> {
        val callbacks = CopyOnWriteArrayList<ThumbData>()
        val task = TransferTask(attachment, attachment.messageTid, attachment.transferState)
        task.thumbCallback = ThumbCallback { _, thumbData -> callbacks.add(thumbData) }
        whenever(fileTransferService.findOrCreateTransferTask(any())).thenReturn(task)
        return callbacks
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

    private fun thumbData(thumbFor: ThumbFor) = ThumbData(
        key = thumbFor.value,
        filePath = "/downloads/image.jpg",
        size = Size(120, 120)
    )

    private fun attachment() = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = "https://cdn.test/image.jpg",
        filePath = "/downloads/image.jpg",
        transferState = TransferState.Downloaded,
        progressPercent = 100f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
