package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.attachment.AttachmentsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AttachmentTransferResetCacheTest {

    @Test
    fun `messages cache reset moves completed attachment to pending download`() = runTest {
        val cache = MessagesCache()
        cache.add(CHANNEL_ID, messageWith(attachment()))

        cache.resetAttachmentTransferData(pendingDownload())

        val attachment = cache.get(CHANNEL_ID, MESSAGE_TID)?.attachments?.single()
        assertThat(attachment?.transferState).isEqualTo(TransferState.PendingDownload)
        assertThat(attachment?.progressPercent).isEqualTo(0f)
        assertThat(attachment?.filePath).isNull()
    }

    @Test
    fun `messages cache update still rejects completed to pending download`() = runTest {
        val cache = MessagesCache()
        cache.add(CHANNEL_ID, messageWith(attachment()))

        cache.updateAttachmentTransferData(pendingDownload())

        val attachment = cache.get(CHANNEL_ID, MESSAGE_TID)?.attachments?.single()
        assertThat(attachment?.transferState).isEqualTo(TransferState.Uploaded)
        assertThat(attachment?.filePath).isEqualTo(DELETED_PATH)
    }

    @Test
    fun `attachments cache reset moves completed attachment to pending download`() = runTest {
        val cache = AttachmentsCache()
        cache.add(attachment())

        cache.resetAttachmentTransferData(pendingDownload())

        val attachment = cache.get(AttachmentTypeEnum.Image.value, MESSAGE_TID)
        assertThat(attachment?.transferState).isEqualTo(TransferState.PendingDownload)
        assertThat(attachment?.filePath).isNull()
    }

    @Test
    fun `attachments cache update still rejects completed to pending download`() = runTest {
        val cache = AttachmentsCache()
        cache.add(attachment())

        cache.updateAttachmentTransferData(pendingDownload())

        val attachment = cache.get(AttachmentTypeEnum.Image.value, MESSAGE_TID)
        assertThat(attachment?.transferState).isEqualTo(TransferState.Uploaded)
    }

    private fun messageWith(attachment: SceytAttachment) =
        createMessage(createdAt = 1_000L, id = MESSAGE_TID, tid = MESSAGE_TID).copy(attachments = listOf(attachment))

    private fun pendingDownload() = TransferData(
        messageTid = MESSAGE_TID,
        progressPercent = 0f,
        state = TransferState.PendingDownload,
        filePath = null,
        url = URL,
    )

    private fun attachment() = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = URL,
        filePath = DELETED_PATH,
        transferState = TransferState.Uploaded,
        progressPercent = 100f,
        originalFilePath = DELETED_PATH,
        linkPreviewDetails = null,
    )

    private companion object {
        const val CHANNEL_ID = 1L
        const val MESSAGE_TID = 10L
        const val URL = "https://cdn.test/image.jpg"
        const val DELETED_PATH = "/gallery/deleted.jpg"
    }
}
