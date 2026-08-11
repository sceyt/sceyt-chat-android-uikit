package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import org.junit.Test

class FileListItemTest {

    @Test
    fun `copy can be mutated without changing its source`() {
        val source = FileListItem(
            _attachment = attachment(filePath = "/files/original.jpg"),
            _metadataPayload = AttachmentMetadataPayload(
                duration = 1_000,
            ),
            _thumbPath = "/thumbs/original.jpg",
            _transferData = transfer(filePath = "/files/original.jpg"),
            type = AttachmentTypeEnum.Image,
        )

        val copy = source.copy()

        assertThat(copy).isEqualTo(source)
        assertThat(copy).isNotSameInstanceAs(source)
        assertThat(copy.attachment).isEqualTo(source.attachment)
        assertThat(copy.size).isEqualTo(source.size)
        assertThat(copy.duration).isEqualTo(source.duration)
        assertThat(copy.blurredThumb).isEqualTo(source.blurredThumb)
        assertThat(copy.audioMetadata).isEqualTo(source.audioMetadata)
        assertThat(copy.thumbPath).isEqualTo(source.thumbPath)
        assertThat(copy.transferData).isEqualTo(source.transferData)
        assertThat(copy.transferData).isNotSameInstanceAs(source.transferData)
        assertThat(copy.type).isEqualTo(source.type)

        copy.updateAttachment(attachment(filePath = "/files/updated.jpg"))
        copy.updateThumbPath("/thumbs/updated.jpg")
        copy.transferData?.apply {
            state = TransferState.ErrorDownload
            filePath = "/files/failed.jpg"
        }

        assertThat(source.attachment.filePath).isEqualTo("/files/original.jpg")
        assertThat(source.thumbPath).isEqualTo("/thumbs/original.jpg")
        assertThat(source.transferData?.state).isEqualTo(TransferState.Downloaded)
        assertThat(source.transferData?.filePath).isEqualTo("/files/original.jpg")

        source.updateThumbPath("/thumbs/source-new.jpg")
        source.transferData?.filePath = "/files/source-new.jpg"

        assertThat(copy.thumbPath).isEqualTo("/thumbs/updated.jpg")
        assertThat(copy.transferData?.filePath).isEqualTo("/files/failed.jpg")
    }

    private fun attachment(filePath: String) = SceytAttachment(
        id = 1,
        messageId = 10,
        messageTid = 100,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 1_000,
        createdAt = 1_000,
        url = "https://cdn.test/image.jpg",
        filePath = filePath,
        transferState = TransferState.Downloaded,
        progressPercent = 100f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun transfer(filePath: String) = TransferData(
        messageTid = 100,
        progressPercent = 100f,
        state = TransferState.Downloaded,
        filePath = filePath,
        url = "https://cdn.test/image.jpg",
    )
}
