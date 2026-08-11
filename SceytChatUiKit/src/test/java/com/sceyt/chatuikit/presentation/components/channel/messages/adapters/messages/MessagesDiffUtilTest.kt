package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentMetadataPayload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import org.junit.Test
import org.mockito.kotlin.mock

class MessagesDiffUtilTest {

    private fun messageItem(
        tid: Long,
        id: Long = tid,
        body: String = "",
    ) = MessageListItem.MessageItem(
        createMessage(createdAt = id, id = id, tid = tid).copy(body = body)
    )

    @Test
    fun `message identity uses tid and exposes payload`() {
        val oldItem = messageItem(tid = 10, id = 1, body = "old")
        val newItem = messageItem(tid = 10, id = 2, body = "new")
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areItemsTheSame(0, 0)).isTrue()
        assertThat(diff.areContentsTheSame(0, 0)).isFalse()

        val payload = diff.getChangePayload(0, 0) as MessageDiff
        assertThat(payload.bodyChanged).isTrue()
    }

    @Test
    fun `different message tids are different items`() {
        val diff = MessagesDiffUtil(
            oldList = listOf(messageItem(tid = 10)),
            newList = listOf(messageItem(tid = 11))
        )

        assertThat(diff.areItemsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `date separator identity changes when date changes`() {
        val oldItem = MessageListItem.DateSeparatorItem(
            createdAt = 1,
            messageTid = 10,
            messageId = 100
        )
        val newItem = oldItem.copy(createdAt = 2)
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areItemsTheSame(0, 0)).isFalse()
        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `loading items keep stable identity`() {
        val prevDiff = MessagesDiffUtil(
            oldList = listOf(MessageListItem.LoadingPrevItem),
            newList = listOf(MessageListItem.LoadingPrevItem)
        )
        val nextDiff = MessagesDiffUtil(
            oldList = listOf(MessageListItem.LoadingNextItem),
            newList = listOf(MessageListItem.LoadingNextItem)
        )

        assertThat(prevDiff.areItemsTheSame(0, 0)).isTrue()
        assertThat(prevDiff.areContentsTheSame(0, 0)).isTrue()
        assertThat(nextDiff.areItemsTheSame(0, 0)).isTrue()
        assertThat(nextDiff.areContentsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `terminal attachment state change produces files payload`() {
        val oldAttachment = attachment(
            state = TransferState.Downloading,
            progress = 50f,
        )
        val newAttachment = oldAttachment.copy(
            transferState = TransferState.Downloaded,
        )
        val oldItem = messageItem(tid = 10).withFiles(
            attachment = oldAttachment,
            file = fileItem(oldAttachment, TransferState.Downloading, 50f),
        )
        val newItem = messageItem(tid = 10).withFiles(
            attachment = newAttachment,
            file = fileItem(
                attachment = newAttachment,
                state = TransferState.Downloaded,
                progress = 50f,
            ),
        )
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).filesChanged).isTrue()
    }

    @Test
    fun `attachment path change produces files payload`() {
        val oldAttachment = attachment()
        val newAttachment = oldAttachment.copy(filePath = "/files/downloaded.jpg")
        val oldItem = messageItem(tid = 10).withFiles(oldAttachment)
        val newItem = messageItem(tid = 10).withFiles(newAttachment)
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).filesChanged).isTrue()
    }

    @Test
    fun `progress only attachment change does not produce payload`() {
        val oldAttachment = attachment(
            state = TransferState.Downloading,
            progress = 10f,
        )
        val newAttachment = oldAttachment.copy(progressPercent = 60f)
        val oldItem = messageItem(tid = 10).withFiles(
            attachment = oldAttachment,
            file = fileItem(oldAttachment, TransferState.Downloading, 10f),
        )
        val newItem = messageItem(tid = 10).withFiles(
            attachment = newAttachment,
            file = fileItem(newAttachment, TransferState.Downloading, 60f),
        )
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `file overlay state change produces files payload`() {
        val domainAttachment = attachment()
        val oldFileAttachment = domainAttachment.copy(
            transferState = TransferState.Downloading,
            progressPercent = 50f,
        )
        val newFileAttachment = oldFileAttachment.copy(
            transferState = TransferState.Downloaded,
        )
        val oldItem = messageItem(tid = 10).withFiles(
            attachment = domainAttachment,
            file = fileItem(oldFileAttachment, TransferState.Downloading, 50f),
        )
        val newItem = messageItem(tid = 10).withFiles(
            attachment = domainAttachment,
            file = fileItem(newFileAttachment, TransferState.Downloaded, 50f),
        )
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).filesChanged).isTrue()
    }

    @Test
    fun `thumb path change produces files payload`() {
        val attachment = attachment()
        val oldItem = messageItem(tid = 10).withFiles(
            attachment = attachment,
            file = fileItem(attachment, thumbPath = "/thumbs/old.jpg"),
        )
        val newItem = messageItem(tid = 10).withFiles(
            attachment = attachment,
            file = fileItem(attachment, thumbPath = "/thumbs/new.jpg"),
        )
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).filesChanged).isTrue()
    }

    @Test
    fun `link preview change produces files payload`() {
        val oldAttachment = attachment(
            type = AttachmentTypeEnum.Link,
            linkPreview = LinkPreviewDetails.hiddenLink("https://example.test/old"),
        )
        val newAttachment = oldAttachment.copy(
            linkPreviewDetails = LinkPreviewDetails.hiddenLink("https://example.test/new")
        )
        val oldItem = messageItem(tid = 10).withFiles(oldAttachment)
        val newItem = messageItem(tid = 10).withFiles(newAttachment)
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).filesChanged).isTrue()
    }

    @Test
    fun `downloaded parent attachment produces reply payload`() {
        val oldAttachment = attachment(messageTid = 100)
        val newAttachment = oldAttachment.copy(
            filePath = "/files/parent.jpg",
            transferState = TransferState.Downloaded,
            progressPercent = 100f,
        )
        val parent = createMessage(createdAt = 100, id = 100, tid = 100)
        val oldReply = messageItem(tid = 10).let { item ->
            item.copy(message = item.message.copy(
                parentMessage = parent.copy(attachments = listOf(oldAttachment))
            ))
        }
        val newReply = oldReply.copy(message = oldReply.message.copy(
            parentMessage = parent.copy(attachments = listOf(newAttachment))
        ))
        val diff = MessagesDiffUtil(listOf(oldReply), listOf(newReply))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).replyContainerChanged).isTrue()
    }

    @Test
    fun `parent server id change produces reply payload`() {
        val parent = createMessage(createdAt = 100, id = 0, tid = 100)
        val oldReply = messageItem(tid = 10).let { item ->
            item.copy(message = item.message.copy(parentMessage = parent))
        }
        val newReply = oldReply.copy(message = oldReply.message.copy(
            parentMessage = parent.copy(id = 100)
        ))
        val diff = MessagesDiffUtil(listOf(oldReply), listOf(newReply))

        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
        assertThat((diff.getChangePayload(0, 0) as MessageDiff).replyContainerChanged).isTrue()
    }

    @Test
    fun `parent attachment progress only change does not produce payload`() {
        val oldAttachment = attachment(
            messageTid = 100,
            state = TransferState.Downloading,
            progress = 10f,
        )
        val newAttachment = oldAttachment.copy(progressPercent = 60f)
        val parent = createMessage(createdAt = 100, id = 100, tid = 100)
        val oldReply = messageItem(tid = 10).let { item ->
            item.copy(message = item.message.copy(
                parentMessage = parent.copy(attachments = listOf(oldAttachment))
            ))
        }
        val newReply = oldReply.copy(message = oldReply.message.copy(
            parentMessage = parent.copy(attachments = listOf(newAttachment))
        ))
        val diff = MessagesDiffUtil(listOf(oldReply), listOf(newReply))

        assertThat(diff.areContentsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `equivalent remapped bitmap metadata does not produce files payload`() {
        val attachment = attachment()
        val oldItem = messageItem(tid = 10).withFiles(
            attachment = attachment,
            file = fileItem(
                attachment = attachment,
                metadataPayload = AttachmentMetadataPayload(blurredThumbBitmap = mock<Bitmap>()),
            ),
        )
        val newItem = messageItem(tid = 10).withFiles(
            attachment = attachment,
            file = fileItem(
                attachment = attachment,
                metadataPayload = AttachmentMetadataPayload(blurredThumbBitmap = mock<Bitmap>()),
            ),
        )
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areContentsTheSame(0, 0)).isTrue()
    }

    private fun MessageListItem.MessageItem.withFiles(
        attachment: SceytAttachment,
        file: FileListItem? = null,
    ) = copy(message = message.copy(
        attachments = listOf(attachment),
        files = file?.let(::listOf),
    ))

    private fun fileItem(
        attachment: SceytAttachment,
        state: TransferState = TransferState.Downloaded,
        progress: Float = 100f,
        thumbPath: String? = null,
        transferFilePath: String? = attachment.filePath,
        metadataPayload: AttachmentMetadataPayload = AttachmentMetadataPayload(),
    ) = FileListItem(
        _attachment = attachment,
        _metadataPayload = metadataPayload,
        _thumbPath = thumbPath,
        _transferData = TransferData(
            messageTid = attachment.messageTid,
            progressPercent = progress,
            state = state,
            filePath = transferFilePath,
            url = attachment.url,
        ),
        type = AttachmentTypeEnum.Image,
    )

    private fun attachment(
        messageTid: Long = 10,
        type: AttachmentTypeEnum = AttachmentTypeEnum.Image,
        state: TransferState = TransferState.PendingDownload,
        progress: Float = 0f,
        linkPreview: LinkPreviewDetails? = null,
    ) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "file-$messageTid",
        type = type.value,
        metadata = null,
        fileSize = 1_000,
        createdAt = 1_000,
        url = "https://cdn.test/file-$messageTid",
        filePath = null,
        transferState = state,
        progressPercent = progress,
        originalFilePath = null,
        linkPreviewDetails = linkPreview,
    )
}
