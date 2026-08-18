package com.sceyt.chatuikit.presentation.components.channel.messages.events

import android.graphics.Bitmap
import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentDataProviderVideoThumbTest {

    @Test
    fun `video thumb url is taken from the attachment metadata`() {
        val item = TestItem(attachment(metadata = """{"video_thumb":"https://sceyt.com/t.jpg"}"""))

        assertThat(item.videoThumbUrl).isEqualTo("https://sceyt.com/t.jpg")
    }

    @Test
    fun `video thumb url is null without the metadata key`() {
        assertThat(TestItem(attachment(metadata = null)).videoThumbUrl).isNull()
        assertThat(TestItem(attachment(metadata = """{"tmb":"abc"}""")).videoThumbUrl).isNull()
    }

    @Test
    fun `video thumb url is not read for a non video attachment`() {
        val item = TestItem(
            attachment(
                metadata = """{"video_thumb":"https://sceyt.com/t.jpg"}""",
                type = AttachmentTypeEnum.Image.value
            )
        )

        assertThat(item.videoThumbUrl).isNull()
    }

    @Test
    fun `video thumb url follows the updated attachment`() {
        val item = TestItem(attachment(metadata = null))
        assertThat(item.videoThumbUrl).isNull()

        item.updateAttachment(attachment(metadata = """{"video_thumb":"https://sceyt.com/t.jpg"}"""))

        assertThat(item.videoThumbUrl).isEqualTo("https://sceyt.com/t.jpg")
    }

    private fun attachment(
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
        url = "https://sceyt.com/video.mp4",
        filePath = null,
        transferState = TransferState.PendingDownload,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private class TestItem(
        private var currentAttachment: SceytAttachment,
    ) : AttachmentDataProvider {
        override val attachment: SceytAttachment get() = currentAttachment
        override val size: Size? get() = null
        override val blurredThumb: Bitmap? get() = null
        override val thumbPath: String? get() = null
        override val duration: Long? get() = null
        override val audioMetadata: AudioMetadata? get() = null
        override val transferData: TransferData? get() = null

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            currentAttachment = file
            return currentAttachment
        }

        override fun updateTransferData(transferData: TransferData?) = Unit
        override fun updateThumbPath(thumbPath: String?) = Unit
    }
}
