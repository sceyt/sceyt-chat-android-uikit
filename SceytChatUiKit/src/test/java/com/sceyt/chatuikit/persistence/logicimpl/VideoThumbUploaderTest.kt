package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class VideoThumbUploaderTest {
    private val context = RuntimeEnvironment.getApplication()
    private val transferUtility = mock<FileTransferUtility>()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()

    @Test
    fun `uploads the thumb once per file and applies its url to the attachment metadata`() {
        val uploadCount = stubThumbUpload(SceytResponse.Success(THUMB_URL))
        val uploader = createUploader()
        val attachment = attachment()
        val task = task(attachment)

        uploader.start(attachment)
        uploader.start(attachment)
        val result = deliverResult(uploader, attachment, task, SceytResponse.Success(VIDEO_URL))

        assertThat(uploadCount.get()).isEqualTo(1)
        assertThat((result as SceytResponse.Success).data).isEqualTo(VIDEO_URL)
        assertThat(task.attachment.getVideoThumbUrl()).isEqualTo(THUMB_URL)
        assertThat(task.attachment.url).isEqualTo(VIDEO_URL)
    }

    @Test
    fun `reports an error, but keeps the uploaded video url, when the thumb upload fails`() {
        stubThumbUpload(SceytResponse.Error(SceytException(0, "No connection")))
        val uploader = createUploader()
        val attachment = attachment()
        val task = task(attachment)

        uploader.start(attachment)
        val result = deliverResult(uploader, attachment, task, SceytResponse.Success(VIDEO_URL))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        // The url is kept, so the retry doesn't upload the video again
        assertThat(task.attachment.url).isEqualTo(VIDEO_URL)
        assertThat(task.attachment.metadata).isNull()
    }

    @Test
    fun `reports an error when the frame extraction fails`() {
        val uploader = createUploader(thumbFileProvider = { Result.failure(Throwable("No track")) })
        val attachment = attachment()
        val task = task(attachment)

        uploader.start(attachment)
        val result = deliverResult(uploader, attachment, task, SceytResponse.Success(VIDEO_URL))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
    }

    @Test
    fun `delivers the result as is when the thumb is already uploaded`() {
        val uploadCount = stubThumbUpload(SceytResponse.Success(THUMB_URL))
        val uploader = createUploader()
        val attachment = attachment(metadata = """{"video_thumb":"$THUMB_URL"}""")
        val task = task(attachment)

        uploader.start(attachment)
        val result = deliverResult(uploader, attachment, task, SceytResponse.Success(VIDEO_URL))

        assertThat(uploadCount.get()).isEqualTo(0)
        assertThat((result as SceytResponse.Success).data).isEqualTo(VIDEO_URL)
    }

    @Test
    fun `delivers a failed file upload without waiting for the thumb`() {
        stubThumbUpload(SceytResponse.Success(THUMB_URL))
        val uploader = createUploader()
        val attachment = attachment()
        val task = task(attachment)

        val result = deliverResult(uploader, attachment, task, SceytResponse.Error(SceytException(0, "Failed")))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(task.attachment.url).isNull()
    }

    @Test
    fun `applies one thumb upload to every task sharing the same file`() {
        val uploadCount = stubThumbUpload(SceytResponse.Success(THUMB_URL))
        val uploader = createUploader()
        val attachment = attachment()
        val firstTask = task(attachment)
        val secondTask = task(attachment.copy(messageTid = 2L))

        uploader.start(attachment)
        val latch = CountDownLatch(2)
        listOf(firstTask, secondTask).forEach { task ->
            task.uploadResultCallback = TransferResultCallback { latch.countDown() }
        }
        uploader.deliverResult(attachment, listOf(firstTask, secondTask), SceytResponse.Success(VIDEO_URL))

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(uploadCount.get()).isEqualTo(1)
        assertThat(firstTask.attachment.getVideoThumbUrl()).isEqualTo(THUMB_URL)
        assertThat(secondTask.attachment.getVideoThumbUrl()).isEqualTo(THUMB_URL)
    }

    @Test
    fun `cancel drops the upload and allows starting it again`() {
        val uploadCount = stubThumbUpload(SceytResponse.Success(THUMB_URL))
        val uploader = createUploader()
        val attachment = attachment()

        uploader.start(attachment)
        uploader.cancel(attachment)
        uploader.start(attachment)
        deliverResult(uploader, attachment, task(attachment), SceytResponse.Success(VIDEO_URL))

        assertThat(uploadCount.get()).isEqualTo(2)
    }

    @Test
    fun `the thumb file is deleted after the upload`() {
        val thumbFile = File.createTempFile("thumb", ".jpg")
        whenever(transferUtility.uploadFileByPath(any(), eq(thumbFile.path), any())).thenAnswer {
            it.thumbCallback(SceytResponse.Success(THUMB_URL))
        }
        val uploader = createUploader(thumbFileProvider = { Result.success(thumbFile) })
        val attachment = attachment()

        uploader.start(attachment)
        deliverResult(uploader, attachment, task(attachment), SceytResponse.Success(VIDEO_URL))

        assertThat(thumbFile.exists()).isFalse()
    }

    private fun stubThumbUpload(response: SceytResponse<String>): AtomicInteger {
        val count = AtomicInteger()
        whenever(transferUtility.uploadFileByPath(any(), any(), any())).thenAnswer {
            count.incrementAndGet()
            it.thumbCallback(response)
        }
        return count
    }

    @Suppress("UNCHECKED_CAST")
    private fun org.mockito.invocation.InvocationOnMock.thumbCallback(response: SceytResponse<String>) {
        (arguments[2] as (SceytResponse<String>) -> Unit).invoke(response)
    }

    private fun deliverResult(
        uploader: VideoThumbUploader,
        attachment: SceytAttachment,
        task: TransferTask,
        response: SceytResponse<String>,
    ): SceytResponse<String> {
        val latch = CountDownLatch(1)
        var result: SceytResponse<String>? = null
        task.uploadResultCallback = TransferResultCallback {
            result = it
            latch.countDown()
        }
        uploader.deliverResult(attachment, listOf(task), response)
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        return requireNotNull(result)
    }

    private fun createUploader(
        thumbFileProvider: (String) -> Result<File> = {
            Result.success(File.createTempFile("thumb", ".jpg"))
        }
    ) = VideoThumbUploader(context, attachmentLogic, transferUtility, thumbFileProvider)

    private fun task(attachment: SceytAttachment) = TransferTask(
        attachment = attachment,
        messageTid = attachment.messageTid,
        state = attachment.transferState
    )

    private fun attachment(
        metadata: String? = null,
        type: String = AttachmentTypeEnum.Video.value,
    ): SceytAttachment {
        val file = File.createTempFile("video", ".mp4")
        return SceytAttachment(
            id = 1L,
            messageId = 1L,
            messageTid = 1L,
            userId = null,
            name = "video.mp4",
            type = type,
            metadata = metadata,
            fileSize = 100L,
            createdAt = 1_000L,
            url = null,
            filePath = file.path,
            transferState = TransferState.WaitingToUpload,
            progressPercent = 0f,
            originalFilePath = file.path,
            linkPreviewDetails = null,
        )
    }

    private companion object {
        const val VIDEO_URL = "https://sceyt.com/video.mp4"
        const val THUMB_URL = "https://sceyt.com/thumb.jpg"
    }
}