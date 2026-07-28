package com.sceyt.chatuikit.persistence.file_transfer

import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentTransferStateStoreTest {

    @Before
    fun setUp() {
        AttachmentTransferStateStore.clear()
    }

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
    }

    @Test
    fun `progress update cannot move backwards`() {
        val attachment = attachment(url = "https://cdn.test/file")

        assertThat(
            AttachmentTransferStateStore.put(
                transfer(progress = 50f, state = TransferState.Downloading, url = attachment.url)
            )
        ).isNotNull()

        assertThat(
            AttachmentTransferStateStore.put(
                transfer(progress = 40f, state = TransferState.Downloading, url = attachment.url)
            )
        ).isNull()

        val latest = AttachmentTransferStateStore.getTransferData(attachment)
        assertThat(latest?.state).isEqualTo(TransferState.Downloading)
        assertThat(latest?.progressPercent).isEqualTo(50f)
    }

    @Test
    fun `completed transfer cannot regress to running state`() {
        val attachment = attachment(
            url = "https://cdn.test/file",
            filePath = "/downloads/file.jpg",
            state = TransferState.Downloaded,
            progress = 100f
        )

        AttachmentTransferStateStore.put(
            transfer(
                progress = 100f,
                state = TransferState.Downloaded,
                filePath = attachment.filePath,
                url = attachment.url
            )
        )

        val rejected = AttachmentTransferStateStore.put(
            transfer(progress = 20f, state = TransferState.Downloading, url = attachment.url)
        )

        assertThat(rejected).isNull()
        val latest = AttachmentTransferStateStore.getTransferData(attachment)
        assertThat(latest?.state).isEqualTo(TransferState.Downloaded)
        assertThat(latest?.filePath).isEqualTo("/downloads/file.jpg")
    }

    @Test
    fun `file path changed before upload moves entry to resized file path`() {
        val attachment = attachment(
            filePath = "/uploads/original.jpg",
            url = null,
            state = TransferState.PendingUpload,
            progress = 0f
        )

        AttachmentTransferStateStore.put(
            transfer(
                progress = 0f,
                state = TransferState.PendingUpload,
                filePath = attachment.filePath,
                url = null
            )
        )
        val changedPath = AttachmentTransferStateStore.put(
            transfer(
                progress = 0f,
                state = TransferState.FilePathChanged,
                filePath = "/uploads/resized.jpg",
                url = null
            )
        )
        val uploading = AttachmentTransferStateStore.put(
            transfer(
                progress = 25f,
                state = TransferState.Uploading,
                filePath = "/uploads/resized.jpg",
                url = null
            )
        )

        assertThat(changedPath?.state).isEqualTo(TransferState.FilePathChanged)
        assertThat(uploading?.state).isEqualTo(TransferState.Uploading)
        val latest = AttachmentTransferStateStore.getTransferData(
            attachment.copy(filePath = "/uploads/resized.jpg")
        )
        assertThat(latest?.state).isEqualTo(TransferState.Uploading)
        assertThat(latest?.progressPercent).isEqualTo(25f)
        assertThat(latest?.filePath).isEqualTo("/uploads/resized.jpg")
    }

    @Test
    fun `thumb loaded stores thumb without replacing transfer state`() {
        val thumbSize = Size(120, 90)
        val attachment = attachment(
            url = "https://cdn.test/file",
            filePath = "/downloads/file.jpg",
            state = TransferState.Downloaded,
            progress = 100f
        )

        AttachmentTransferStateStore.put(
            transfer(
                progress = 100f,
                state = TransferState.Downloaded,
                filePath = attachment.filePath,
                url = attachment.url
            )
        )
        AttachmentTransferStateStore.put(
            transfer(
                progress = 100f,
                state = TransferState.ThumbLoaded,
                filePath = "/thumbs/file.jpg",
                url = attachment.url,
                thumbData = ThumbData(ThumbFor.MessagesLisView.value, attachment.filePath, thumbSize)
            )
        )

        val latest = AttachmentTransferStateStore.getTransferData(attachment)
        assertThat(latest?.state).isEqualTo(TransferState.Downloaded)
        assertThat(
            AttachmentTransferStateStore.getThumbPath(attachment, ThumbFor.MessagesLisView, thumbSize)
        ).isEqualTo("/thumbs/file.jpg")
    }

    @Test
    fun `thumb lookup falls back to latest target thumb when requested size differs`() {
        val generatedThumbSize = Size(120, 90)
        val requestedThumbSize = Size(240, 180)
        val attachment = attachment(
            url = "https://cdn.test/file",
            filePath = "/downloads/file.jpg",
            state = TransferState.Downloaded,
            progress = 100f
        )

        AttachmentTransferStateStore.put(
            transfer(
                progress = 100f,
                state = TransferState.Downloaded,
                filePath = attachment.filePath,
                url = attachment.url
            )
        )
        AttachmentTransferStateStore.put(
            transfer(
                progress = 100f,
                state = TransferState.ThumbLoaded,
                filePath = "/thumbs/channel-file.jpg",
                url = attachment.url,
                thumbData = ThumbData(ThumbFor.ChannelInfo.value, attachment.filePath, generatedThumbSize)
            )
        )

        assertThat(
            AttachmentTransferStateStore.getThumbPath(attachment, ThumbFor.ChannelInfo, requestedThumbSize)
        ).isEqualTo("/thumbs/channel-file.jpg")
    }

    @Test
    fun `blank update path does not overwrite existing local path`() {
        val attachment = attachment(
            url = "https://cdn.test/file",
            filePath = "/downloads/file.jpg",
            state = TransferState.Downloading,
            progress = 30f
        )

        AttachmentTransferStateStore.put(
            transfer(progress = 40f, state = TransferState.Downloading, url = attachment.url)
        )

        val latest = AttachmentTransferStateStore.getTransferData(attachment)
        assertThat(latest?.filePath).isEqualTo("/downloads/file.jpg")
    }

    @Test
    fun `store evicts least recently used entries when capacity is exceeded`() {
        repeat(513) { index ->
            AttachmentTransferStateStore.put(
                transfer(
                    messageTid = index.toLong(),
                    progress = 30f,
                    state = TransferState.Downloading,
                    url = "https://cdn.test/file-$index"
                )
            )
        }

        assertThat(
            AttachmentTransferStateStore.getTransferData(
                attachment(messageTid = 0, url = "https://cdn.test/file-0")
            )
        ).isNull()
        assertThat(
            AttachmentTransferStateStore.getTransferData(
                attachment(messageTid = 512, url = "https://cdn.test/file-512")
            )?.state
        ).isEqualTo(TransferState.Downloading)
    }

    private fun transfer(
        messageTid: Long = MESSAGE_TID,
        progress: Float,
        state: TransferState,
        filePath: String? = null,
        url: String? = "https://cdn.test/file",
        thumbData: ThumbData? = null,
    ) = TransferData(
        messageTid = messageTid,
        progressPercent = progress,
        state = state,
        filePath = filePath,
        url = url,
        thumbData = thumbData
    )

    private fun attachment(
        messageTid: Long = MESSAGE_TID,
        filePath: String? = null,
        url: String? = "https://cdn.test/file",
        state: TransferState? = TransferState.PendingDownload,
        progress: Float? = 0f,
    ) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "file-$messageTid",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = url,
        filePath = filePath,
        transferState = state,
        progressPercent = progress,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
