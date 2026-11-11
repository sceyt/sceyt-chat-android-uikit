package com.sceyt.chatuikit.persistence.logicimpl.attachment

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AttachmentUploadPersistenceTest {
    private val attachmentDao = mock<AttachmentDao>()
    private val messagesCache = mock<MessagesCache>()
    private val attachmentsCache = AttachmentsCache()
    private val logic = PersistenceAttachmentLogicImpl(
        messageDao = mock(),
        attachmentDao = attachmentDao,
        userDao = mock(),
        fileChecksumDao = mock(),
        linkDao = mock(),
        messagesCache = messagesCache,
        attachmentsCache = attachmentsCache,
        attachmentsRepository = mock(),
    )

    @Test
    fun `upload completion does not insert an attachment into another gallerys cache`() = runTest {
        val galleryAttachment = attachment(messageTid = 1L)
        val uploaded = attachment(messageTid = 2L).copy(
            id = null,
            metadata = """{"enc":{"v":1,"alg":"format","attachmentId":"id","key":"key"}}""",
        )
        attachmentsCache.addAll(listOf(galleryAttachment), checkDifference = false)

        logic.updateUploadedAttachment(uploaded)

        assertThat(attachmentsCache.getSorted(listOf(galleryAttachment.type)))
            .containsExactly(galleryAttachment)
        verify(attachmentDao).updateUploadedAttachment(
            uploaded.toTransferData(TransferState.Uploaded, 100f), uploaded.fileSize, uploaded.metadata,
        )
    }

    @Test
    fun `upload completion updates an attachment already present in the gallery`() = runTest {
        val original = attachment(state = TransferState.Uploading)
        val uploaded = original.copy(url = "https://cdn.test/uploaded")
        attachmentsCache.addAll(listOf(original), checkDifference = false)

        logic.updateUploadedAttachment(uploaded)

        val cached = attachmentsCache.getSorted(listOf(original.type)).single()
        assertThat(cached.url).isEqualTo(uploaded.url)
        assertThat(cached.transferState).isEqualTo(TransferState.Uploaded)
        assertThat(cached.progressPercent).isEqualTo(100f)
    }
}
