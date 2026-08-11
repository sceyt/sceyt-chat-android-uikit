package com.sceyt.chatuikit.persistence.mappers

import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentMediaTypeTest {

    @Test
    fun `file with image extension is visual media`() {
        assertThat(attachment("photo.jpg").getVisualMediaType())
            .isEqualTo(AttachmentTypeEnum.Image)
    }

    @Test
    fun `file with video extension is visual media`() {
        assertThat(attachment("video.mp4").getVisualMediaType())
            .isEqualTo(AttachmentTypeEnum.Video)
    }

    @Test
    fun `file with image file path is visual media`() {
        assertThat(attachment("file", filePath = "/cache/photo.jpg").getVisualMediaType())
            .isEqualTo(AttachmentTypeEnum.Image)
    }

    @Test
    fun `file with video file path is visual media`() {
        assertThat(attachment("file", filePath = "/cache/video.mp4").getVisualMediaType())
            .isEqualTo(AttachmentTypeEnum.Video)
    }

    @Test
    fun `regular file is not visual media`() {
        assertThat(attachment("document.pdf").getVisualMediaType()).isNull()
    }

    @Test
    fun `image file reads media metadata`() {
        val metadata = """{"${SceytConstants.Width}":320,"${SceytConstants.Height}":180}"""

        assertThat(attachment("photo.jpg", metadata).getInfoFromMetadata().size)
            .isEqualTo(Size(320, 180))
    }

    private fun attachment(
        name: String,
        metadata: String? = null,
        filePath: String? = null,
    ) = SceytAttachment(
        id = 1L,
        messageId = 1L,
        messageTid = 1L,
        userId = null,
        name = name,
        type = AttachmentTypeEnum.File.value,
        metadata = metadata,
        fileSize = 100L,
        createdAt = 1_000L,
        url = null,
        filePath = filePath,
        transferState = null,
        progressPercent = null,
        originalFilePath = null,
        linkPreviewDetails = null,
    )
}
