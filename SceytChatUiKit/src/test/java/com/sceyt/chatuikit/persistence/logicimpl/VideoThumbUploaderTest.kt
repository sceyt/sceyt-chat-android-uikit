package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class VideoThumbUploaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var attachmentLogic: PersistenceAttachmentLogic
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var videoFile: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        transport = RecordingFileTransferTransport()
        attachmentLogic = mock()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@VideoThumbUploaderTest.transport
        }
        videoFile = tempFolder.newFile("clip.mp4").apply { writeText("video") }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
    }

    @Test
    fun `a non video attachment is skipped`() = runTest {
        val attachment = attachment(type = AttachmentTypeEnum.Image.value)

        uploader().uploadThumb(attachment)

        assertThat(transport.uploadCalls).isEmpty()
        verifyNoInteractions(attachmentLogic)
    }

    @Test
    fun `a video which already has a thumb url is skipped`() = runTest {
        val attachment = attachment(
            metadata = """{"video_thumb":"https://sceyt.com/existing.jpg"}"""
        )

        uploader().uploadThumb(attachment)

        assertThat(transport.uploadCalls).isEmpty()
    }

    @Test
    fun `the uploaded url is written to every task, the attachment and the checksum`() = runTest {
        val attachment = attachment()
        val first = transferTask(attachment)
        val second = transferTask(attachment.copy(messageTid = 2L))
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }

        val uploader = uploader { _, _ -> Result.success(thumbFile) }
        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadThumb(attachment)
        }
        transport.uploadCalls.single().succeed("https://sceyt.com/new.jpg")
        uploader.applyThumbUrl(attachment, listOf(first, second), requireNotNull(upload.await()))

        assertThat(first.attachment.getVideoThumbUrl()).isEqualTo("https://sceyt.com/new.jpg")
        assertThat(second.attachment.getVideoThumbUrl()).isEqualTo("https://sceyt.com/new.jpg")
        assertThat(thumbFile.exists()).isFalse()
        verify(attachmentLogic).updateFileChecksumMetadata(eq(videoFile.path), any())
    }

    @Test
    fun `the request carries the thumbnail role and its own operation id`() = runTest {
        val attachment = attachment()
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }
        val uploader = uploader { _, _ -> Result.success(thumbFile) }

        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadThumb(attachment)
        }
        val request = transport.uploadCalls.single().request
        transport.uploadCalls.single().succeed("https://sceyt.com/new.jpg")
        upload.await()

        assertThat(request.role).isEqualTo(TransferRole.Thumbnail)
        assertThat(request.operationId)
            .isEqualTo(uploadOperationId(attachment.messageTid, TransferRole.Thumbnail))
        assertThat(request.operationId).isNotEqualTo(uploadOperationId(attachment.messageTid))
    }

    @Test
    fun `a failed thumb upload retries while its caller is active`() = runTest {
        val attachment = attachment()
        val task = transferTask(attachment)
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }
        val uploader = uploader(retryDelay = Duration.ZERO) { _, _ -> Result.success(thumbFile) }

        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadThumb(attachment)
        }
        transport.uploadCalls.single().fail(IllegalStateException("boom"))
        transport.uploadCalls.last().succeed("https://sceyt.com/retried.jpg")
        uploader.applyThumbUrl(attachment, listOf(task), requireNotNull(upload.await()))

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(task.attachment.getVideoThumbUrl()).isEqualTo("https://sceyt.com/retried.jpg")
        assertThat(thumbFile.exists()).isFalse()
    }

    @Test
    fun `persistence failure does not repeat a successful upload`() = runTest {
        val attachment = attachment()
        val task = transferTask(attachment)
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }
        val uploader = uploader(retryDelay = Duration.ZERO) { _, _ -> Result.success(thumbFile) }
        whenever(attachmentLogic.updateAttachmentMetadata(any(), any()))
            .thenThrow(IllegalStateException("database failed"))

        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadThumb(attachment)
        }
        transport.uploadCalls.single().succeed("https://sceyt.com/thumb.jpg")
        uploader.applyThumbUrl(attachment, listOf(task), requireNotNull(upload.await()))

        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `cancelling the caller stops retries and deletes the extracted frame`() = runTest {
        val attachment = attachment()
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }
        val uploader = uploader { _, _ -> Result.success(thumbFile) }

        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadThumb(attachment)
        }
        upload.cancelAndJoin()

        assertThat(transport.uploadCalls.single().cancelled).isTrue()
        assertThat(thumbFile.exists()).isFalse()
    }

    @Test
    fun `a failed frame extraction skips the upload`() = runTest {
        val attachment = attachment()

        uploader { _, _ -> Result.failure(IllegalStateException("no frame")) }
            .uploadThumb(attachment)

        assertThat(transport.uploadCalls).isEmpty()
    }

    private fun uploader(
        retryDelay: Duration = 1.seconds,
        thumbFileProvider: (Context, String) -> Result<File> = { _, _ ->
            Result.failure(IllegalStateException("not stubbed"))
        },
    ) = VideoThumbUploader(context, attachmentLogic, thumbFileProvider, retryDelay)

    private fun attachment(
        type: String = AttachmentTypeEnum.Video.value,
        metadata: String? = null,
    ) = attachment(
        messageTid = 1L,
        name = "clip.mp4",
        type = type,
        filePath = videoFile.path,
        originalFilePath = videoFile.path,
    ).copy(metadata = metadata)
}
