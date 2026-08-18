package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbCallback
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentDownloadCoordinatorVideoThumbTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var service: TestFileTransferService
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var coordinator: AttachmentDownloadCoordinator
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var videoDestination: File
    private lateinit var thumbDestination: File

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        service = TestFileTransferService()
        transport = RecordingFileTransferTransport()
        videoDestination = File(tempFolder.root, "clip.mp4")
        thumbDestination = File(tempFolder.root, "video_thumb.jpeg")
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentDownloadCoordinatorVideoThumbTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _, role ->
                if (role == TransferRole.Thumbnail) thumbDestination else videoDestination
            }
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
        coordinator = AttachmentDownloadCoordinator(context, TestScope(UnconfinedTestDispatcher()))
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `the poster downloads beside the video under its own operation`() {
        val attachment = videoAttachment(thumbUrl = "https://sceyt.com/poster.jpg")

        coordinator.downloadFile(attachment, taskFor(attachment))

        val roles = transport.downloadCalls.map { it.request.role }
        assertThat(roles).containsExactly(TransferRole.Thumbnail, TransferRole.Main).inOrder()
        assertThat(transport.downloadCalls.map { it.request.operationId }).containsExactly(
            downloadOperationId(attachment.messageTid, TransferRole.Thumbnail),
            downloadOperationId(attachment.messageTid, TransferRole.Main),
        )
        assertThat(transport.downloadCalls.first().request.url)
            .isEqualTo("https://sceyt.com/poster.jpg")
        assertThat(transport.downloadCalls.first().request.destinationFile)
            .isEqualTo(thumbDestination)
    }

    @Test
    fun `a loaded poster is reported to every screen`() {
        val attachment = videoAttachment(thumbUrl = "https://sceyt.com/poster.jpg")
        val task = taskFor(attachment)
        val thumbs = mutableListOf<Pair<String, Int>>()
        task.thumbCallback = ThumbCallback { path, data -> thumbs.add(path to data.key) }

        coordinator.downloadFile(attachment, task)
        thumbDestination.writeText("poster")
        transport.downloadCalls.first().succeed(thumbDestination.path)

        assertThat(thumbs.map { it.second })
            .containsExactlyElementsIn(ThumbFor.entries.map { it.value })
        assertThat(thumbs.map { it.first }.distinct()).containsExactly(thumbDestination.path)
    }

    @Test
    fun `an already downloaded poster is served without a transfer`() {
        thumbDestination.writeText("poster")
        val attachment = videoAttachment(thumbUrl = "https://sceyt.com/poster.jpg")
        val task = taskFor(attachment)
        val thumbs = mutableListOf<String>()
        task.thumbCallback = ThumbCallback { path, _ -> thumbs.add(path) }

        coordinator.downloadFile(attachment, task)

        assertThat(transport.downloadCalls.map { it.request.role }).containsExactly(TransferRole.Main)
        assertThat(thumbs.distinct()).containsExactly(thumbDestination.path)
    }

    @Test
    fun `a video without a poster url downloads only itself`() {
        val attachment = videoAttachment(thumbUrl = null)

        coordinator.downloadFile(attachment, taskFor(attachment))

        assertThat(transport.downloadCalls.map { it.request.role }).containsExactly(TransferRole.Main)
    }

    @Test
    fun `a poster is not downloaded when the video is already local`() {
        val localVideo = tempFolder.newFile("local.mp4").apply { writeText("video") }
        val attachment = videoAttachment(thumbUrl = "https://sceyt.com/poster.jpg")
            .copy(filePath = localVideo.path)

        coordinator.downloadFile(attachment, taskFor(attachment))

        assertThat(transport.downloadCalls.map { it.request.role }).containsExactly(TransferRole.Main)
    }

    @Test
    fun `a failed poster download drops its partial file and leaves the video running`() {
        val attachment = videoAttachment(thumbUrl = "https://sceyt.com/poster.jpg")

        coordinator.downloadFile(attachment, taskFor(attachment))
        thumbDestination.writeText("partial")
        transport.downloadCalls.first().fail(IllegalStateException("boom"))

        assertThat(thumbDestination.exists()).isFalse()
        assertThat(transport.downloadCalls.last().request.role).isEqualTo(TransferRole.Main)
        assertThat(transport.downloadCalls.last().result.isCompleted).isFalse()
    }

    private fun taskFor(attachment: SceytAttachment): TransferTask {
        return transferTask(attachment).also(service::addTransferTask)
    }

    private fun videoAttachment(thumbUrl: String?) = attachment(
        messageTid = 42L,
        name = "clip.mp4",
        type = AttachmentTypeEnum.Video.value,
        url = "https://sceyt.com/clip.mp4",
        filePath = null,
        originalFilePath = null,
        state = TransferState.PendingDownload,
    ).copy(metadata = thumbUrl?.let { """{"video_thumb":"$it"}""" })
}