package com.sceyt.chatuikit.persistence.mappers

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class VideoThumbMetadataTest {

    @Test
    fun `video thumb url is null when metadata is empty or has no key`() {
        assertThat(createAttachment(metadata = null).getVideoThumbUrl()).isNull()
        assertThat(createAttachment(metadata = "").getVideoThumbUrl()).isNull()
        assertThat(createAttachment(metadata = """{"tmb":"abc"}""").getVideoThumbUrl()).isNull()
    }

    @Test
    fun `video thumb url is null when metadata is malformed`() {
        val attachment = createAttachment(metadata = "not a json")
        assertThat(attachment.getVideoThumbUrl()).isNull()
        assertThat(attachment.needsVideoThumbUpload()).isTrue()
    }

    @Test
    fun `upsert keeps the existing metadata keys`() {
        val attachment = createAttachment(metadata = """{"tmb":"abc","szw":100,"szh":200}""")

        val metadata = attachment.upsertVideoThumbUrlMetadata("https://sceyt.com/thumb.jpg")

        val json = JSONObject(requireNotNull(metadata))
        assertThat(json.getString(SceytConstants.Thumb)).isEqualTo("abc")
        assertThat(json.getInt(SceytConstants.Width)).isEqualTo(100)
        assertThat(json.getInt(SceytConstants.Height)).isEqualTo(200)
        assertThat(json.getString(SceytConstants.VideoThumbUrl))
            .isEqualTo("https://sceyt.com/thumb.jpg")
    }

    @Test
    fun `upsert creates the metadata when it is missing`() {
        val metadata = createAttachment(metadata = null)
            .upsertVideoThumbUrlMetadata("https://sceyt.com/thumb.jpg")

        assertThat(createAttachment(metadata = metadata).getVideoThumbUrl())
            .isEqualTo("https://sceyt.com/thumb.jpg")
    }

    @Test
    fun `thumb upload is needed only for a video without a thumb url`() {
        val withThumb = createAttachment(metadata = """{"video_thumb":"https://sceyt.com/t.jpg"}""")
        assertThat(withThumb.needsVideoThumbUpload()).isFalse()

        assertThat(createAttachment(metadata = null).needsVideoThumbUpload()).isTrue()

        val image = createAttachment(metadata = null, type = AttachmentTypeEnum.Image.value)
        assertThat(image.needsVideoThumbUpload()).isFalse()
    }

    @Test
    fun `metadata payload exposes the video thumb url`() {
        val attachment = createAttachment(
            metadata = """{"szw":100,"szh":200,"dur":8,"video_thumb":"https://sceyt.com/t.jpg"}"""
        )

        val payload = attachment.getInfoFromMetadata()

        assertThat(payload.videoThumbUrl).isEqualTo("https://sceyt.com/t.jpg")
        assertThat(payload.duration).isEqualTo(8L)
    }

    @Test
    fun `metadata payload of an image has no video thumb url`() {
        val attachment = createAttachment(
            metadata = """{"video_thumb":"https://sceyt.com/t.jpg"}""",
            type = AttachmentTypeEnum.Image.value
        )

        assertThat(attachment.getInfoFromMetadata().videoThumbUrl).isNull()
    }

    @Test
    fun `thumb upload is not needed when the local file is missing`() {
        val attachment = createAttachment(metadata = null).copy(
            filePath = "/not/existing/video.mp4",
            originalFilePath = "/not/existing/video.mp4"
        )

        assertThat(attachment.needsVideoThumbUpload()).isFalse()
    }

    private fun createAttachment(
        metadata: String?,
        type: String = AttachmentTypeEnum.Video.value
    ) = SceytAttachment(
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
        filePath = videoFile.path,
        transferState = TransferState.WaitingToUpload,
        progressPercent = 0f,
        originalFilePath = videoFile.path,
        linkPreviewDetails = null,
    )

    private companion object {
        private val videoFile: File = File.createTempFile("video", ".mp4")
    }
}
