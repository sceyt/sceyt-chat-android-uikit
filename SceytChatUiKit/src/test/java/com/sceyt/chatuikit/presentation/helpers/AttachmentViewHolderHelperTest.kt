package com.sceyt.chatuikit.presentation.helpers

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AttachmentViewHolderHelperTest {

    @Before
    fun setUp() {
        AttachmentTransferStateStore.clear()
    }

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
    }

    @Test
    fun `update transfer data ignores invalid thumb updates`() {
        val item = TestAttachmentItem(attachment())
        val update = transfer(
            state = TransferState.ThumbLoaded,
            filePath = "/thumbs/file.jpg",
            thumbData = ThumbData(ThumbFor.GlobalSearch.value, "/downloads/file.jpg", Size(100, 100))
        )

        val applied = helper().updateTransferData(update, item) { false }

        assertThat(applied).isFalse()
        assertThat(item.thumbPath).isNull()
        assertThat(item.transferData).isNull()
    }

    @Test
    fun `update transfer data applies valid thumb updates`() {
        val item = TestAttachmentItem(attachment())
        val update = transfer(
            state = TransferState.ThumbLoaded,
            filePath = "/thumbs/file.jpg",
            thumbData = ThumbData(ThumbFor.MessagesLisView.value, "/downloads/file.jpg", Size(100, 100))
        )

        val applied = helper().updateTransferData(update, item) { true }

        assertThat(applied).isTrue()
        assertThat(item.thumbPath).isEqualTo("/thumbs/file.jpg")
    }

    @Test
    fun `update transfer data renders latest store data instead of raw event`() {
        val item = TestAttachmentItem(
            attachment(
                url = "https://cdn.test/file.jpg",
                filePath = "/downloads/file.jpg",
                state = TransferState.PendingDownload,
                progress = 0f
            )
        )
        AttachmentTransferStateStore.put(
            transfer(
                progress = 70f,
                state = TransferState.Downloading,
                url = item.attachment.url
            )
        )

        val applied = helper().updateTransferData(
            data = transfer(
                progress = 40f,
                state = TransferState.Downloading,
                url = item.attachment.url
            ),
            item = item,
            isValidThumb = { true }
        )

        assertThat(applied).isTrue()
        assertThat(item.transferData?.progressPercent).isEqualTo(70f)
        assertThat(item.transferData?.filePath).isEqualTo("/downloads/file.jpg")
        assertThat(item.attachment.progressPercent).isEqualTo(70f)
        assertThat(item.attachment.filePath).isEqualTo("/downloads/file.jpg")
    }

    @Test
    fun `draw thumb uses fallback when old attachment has no blurred thumb`() {
        val helper = helper()
        val imageView = ImageView(RuntimeEnvironment.getApplication())
        val fallback = ColorDrawable(Color.RED)
        var requested = false
        helper.bind(TestAttachmentItem(attachment()))

        helper.drawThumbOrRequest(imageView, { requested = true }, fallback)

        assertThat(imageView.drawable).isSameInstanceAs(fallback)
        assertThat(requested).isTrue()
    }

    private fun helper() = AttachmentViewHolderHelper(View(RuntimeEnvironment.getApplication()))

    private fun transfer(
        progress: Float = 100f,
        state: TransferState,
        filePath: String? = null,
        url: String? = "https://cdn.test/file.jpg",
        thumbData: ThumbData? = null,
    ) = TransferData(
        messageTid = MESSAGE_TID,
        progressPercent = progress,
        state = state,
        filePath = filePath,
        url = url,
        thumbData = thumbData
    )

    private fun attachment(
        filePath: String? = null,
        url: String? = "https://cdn.test/file.jpg",
        state: TransferState? = TransferState.PendingDownload,
        progress: Float? = 0f,
    ) = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "file.jpg",
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

    private class TestAttachmentItem(
        private var currentAttachment: SceytAttachment,
        private var currentTransferData: TransferData? = null,
        private var currentThumbPath: String? = null,
    ) : AttachmentDataProvider {
        override val attachment: SceytAttachment get() = currentAttachment
        override val size: Size? get() = null
        override val blurredThumb: Bitmap? get() = null
        override val thumbPath: String? get() = currentThumbPath
        override val duration: Long? get() = null
        override val audioMetadata: AudioMetadata? get() = null
        override val transferData: TransferData? get() = currentTransferData

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            currentAttachment = file
            return currentAttachment
        }

        override fun updateTransferData(transferData: TransferData?) {
            currentTransferData = transferData
        }

        override fun updateThumbPath(thumbPath: String?) {
            currentThumbPath = thumbPath
        }
    }

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
