package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.util.Size
import android.view.View
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentMetadataPayload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BaseMediaMessageViewHolderThumbValidationTest {

    @Test
    fun `message list thumb is valid when target matches even if size changed`() {
        val holder = TestMediaMessageViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.MessagesLisView.value,
                filePath = "/uploads/image.jpg",
                size = Size(120, 120)
            )
        )

        assertThat(valid).isTrue()
    }

    @Test
    fun `thumb is invalid when target differs`() {
        val holder = TestMediaMessageViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.ChannelInfo.value,
                filePath = "/uploads/image.jpg",
                size = Size(240, 240)
            )
        )

        assertThat(valid).isFalse()
    }

    @Test
    fun `holder file item is detached from canonical message`() {
        val attachment = attachment()
        val canonical = FileListItem(
            _attachment = attachment,
            _metadataPayload = AttachmentMetadataPayload(),
            _thumbPath = "/thumbs/canonical.jpg",
            _transferData = transferData(),
            type = AttachmentTypeEnum.Image,
        )
        val messageItem = MessageListItem.MessageItem(
            createMessage(createdAt = 1, id = 1, tid = 1).copy(
                attachments = listOf(attachment),
                files = listOf(canonical),
            )
        )

        val holderItem = TestMediaMessageViewHolder().fileItemFrom(messageItem)!!

        assertThat(holderItem).isNotSameInstanceAs(canonical)
        assertThat(holderItem.transferData).isNotSameInstanceAs(canonical.transferData)

        holderItem.updateAttachment(attachment.copy(filePath = "/files/holder.jpg"))
        holderItem.updateThumbPath("/thumbs/holder.jpg")
        holderItem.transferData?.state = TransferState.ErrorDownload

        assertThat(canonical.attachment.filePath).isNull()
        assertThat(canonical.thumbPath).isEqualTo("/thumbs/canonical.jpg")
        assertThat(canonical.transferData?.state).isEqualTo(TransferState.PendingDownload)
    }

    private fun attachment() = SceytAttachment(
        id = 1,
        messageId = 1,
        messageTid = 1,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 1_000,
        createdAt = 1_000,
        url = "https://cdn.test/image.jpg",
        filePath = null,
        transferState = TransferState.PendingDownload,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun transferData() = TransferData(
        messageTid = 1,
        progressPercent = 0f,
        state = TransferState.PendingDownload,
        filePath = null,
        url = "https://cdn.test/image.jpg",
    )

    private class TestMediaMessageViewHolder : BaseMediaMessageViewHolder(
        view = View(RuntimeEnvironment.getApplication()),
        style = mock<MessageItemStyle>(),
        messageListeners = null,
        needMediaDataCallback = { _: NeedMediaInfoData -> },
    ) {
        fun isThumbValid(data: ThumbData): Boolean = isValidThumb(data)

        fun fileItemFrom(item: MessageListItem.MessageItem): FileListItem? = getFileItem(item)

        override fun getThumbSize(): Size = Size(240, 240)

        override val loadingProgressView: CircularProgressView =
            CircularProgressView(RuntimeEnvironment.getApplication())

        override val selectMessageView: View? = null

        override val incoming: Boolean = false
    }
}
