package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

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

        uploader().uploadAndApplyThumb(attachment, listOf(transferTask(attachment)), null)

        assertThat(transport.uploadCalls).isEmpty()
        verifyNoInteractions(attachmentLogic)
    }

    @Test
    fun `a video which already has a thumb url is skipped`() = runTest {
        val attachment = attachment(
            metadata = """{"video_thumb":"https://sceyt.com/existing.jpg"}"""
        )

        uploader().uploadAndApplyThumb(attachment, listOf(transferTask(attachment)), null)

        assertThat(transport.uploadCalls).isEmpty()
    }

    @Test
    fun `a thumb already uploaded with another message is reused without a transfer`() = runTest {
        val attachment = attachment()
        val task = transferTask(attachment)
        val checksum = checksumData("""{"video_thumb":"https://sceyt.com/shared.jpg"}""")

        uploader().uploadAndApplyThumb(attachment, listOf(task), checksum)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(task.attachment.getVideoThumbUrl()).isEqualTo("https://sceyt.com/shared.jpg")
        verify(attachmentLogic).updateAttachmentMetadata(eq(attachment.messageTid), any())
    }

    @Test
    fun `the uploaded url is written to every task, the attachment and the checksum`() = runTest {
        val attachment = attachment()
        val first = transferTask(attachment)
        val second = transferTask(attachment.copy(messageTid = 2L))
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }

        val uploader = uploader { _, _ -> Result.success(thumbFile) }
        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadAndApplyThumb(attachment, listOf(first, second), null)
        }
        transport.uploadCalls.single().succeed("https://sceyt.com/new.jpg")
        upload.await()

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
            uploader.uploadAndApplyThumb(attachment, listOf(transferTask(attachment)), null)
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
    fun `a failed thumb upload leaves the attachment untouched`() = runTest {
        val attachment = attachment()
        val task = transferTask(attachment)
        val thumbFile = tempFolder.newFile("thumb.jpeg").apply { writeText("thumb") }
        val uploader = uploader { _, _ -> Result.success(thumbFile) }

        val upload = async(Dispatchers.Unconfined) {
            uploader.uploadAndApplyThumb(attachment, listOf(task), null)
        }
        transport.uploadCalls.single().fail(IllegalStateException("boom"))
        upload.await()

        assertThat(task.attachment.getVideoThumbUrl()).isNull()
        assertThat(thumbFile.exists()).isFalse()
    }

    @Test
    fun `a failed frame extraction skips the upload`() = runTest {
        val attachment = attachment()

        uploader { _, _ -> Result.failure(IllegalStateException("no frame")) }
            .uploadAndApplyThumb(attachment, listOf(transferTask(attachment)), null)

        assertThat(transport.uploadCalls).isEmpty()
    }

    private fun uploader(
        thumbFileProvider: (Context, String) -> Result<File> = { _, _ ->
            Result.failure(IllegalStateException("not stubbed"))
        },
    ) = VideoThumbUploader(context, attachmentLogic, thumbFileProvider)

    private fun checksumData(metadata: String?) = FileChecksumData(
        checksum = 1L,
        resizedFilePath = null,
        url = null,
        metadata = metadata,
        fileSize = null,
    )

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