package com.sceyt.chatuikit.persistence.dao.attachmentdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AttachmentDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var messageDao: MessageDao

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java,
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        attachmentDao = database.attachmentsDao()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region Helpers

    private fun message(
        tid: Long,
        id: Long = tid,
        channelId: Long = 1L,
    ) = MessageEntity(
        tid = tid,
        id = id,
        channelId = channelId,
        body = "body",
        type = "text",
        metadata = null,
        createdAt = tid,
        updatedAt = 0,
        incoming = false,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = MessageDeliveryStatus.Displayed,
        state = MessageState.Unmodified,
        fromId = "user1",
        markerCount = null,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        disableMentionsCount = false,
        unList = false,
    )

    private fun attachment(
        id: Long,
        messageTid: Long,
        messageId: Long = messageTid,
        channelId: Long = 1L,
        type: String = AttachmentTypeEnum.Image.value,
        url: String? = "url_$messageTid",
        filePath: String? = "path_$messageTid",
        metadata: String? = "meta_$messageTid",
        fileSize: Long = 100L,
    ) = AttachmentEntity(
        id = id,
        messageId = messageId,
        messageTid = messageTid,
        channelId = channelId,
        userId = "user1",
        name = "attachment_$messageTid",
        type = type,
        metadata = metadata,
        fileSize = fileSize,
        createdAt = id,
        url = url,
        filePath = filePath,
        originalFilePath = null,
        viewOnce = false,
    )

    private fun messageDb(message: MessageEntity, attachments: List<AttachmentEntity>) = MessageDb(
        messageEntity = message,
        from = null,
        parent = null,
        attachments = attachments.map { AttachmentDb(it, null, null) },
        userMarkers = null,
        reactions = null,
        reactionsTotals = null,
        pendingReactions = null,
        forwardingUser = null,
        mentionedUsers = null,
        poll = null,
    )

    private suspend fun insertWithPayload(message: MessageEntity, vararg attachments: AttachmentEntity) {
        messageDao.upsertMessage(messageDb(message, attachments.toList()))
    }

    private suspend fun insertAttachmentsOnly(vararg attachments: AttachmentEntity) {
        val messages = attachments.map {
            message(tid = it.messageTid, id = it.messageId, channelId = it.channelId)
        }
        messageDao.upsertMessageEntitiesWithTransaction(messages)
        attachmentDao.insertAttachments(attachments.toList())
    }

    private suspend fun getAttachment(
        id: Long,
        type: String = AttachmentTypeEnum.Image.value,
        channelId: Long = 1L,
    ): AttachmentEntity {
        return attachmentDao.getNewestThenAttachmentInclude(
            channelId = channelId,
            attachmentId = id,
            limit = 20,
            types = listOf(type),
        ).first { it.attachmentEntity.id == id }.attachmentEntity
    }

    // endregion

    @Test
    fun getAllAttachmentPayLoadsByMsgTid_returnsOnlyRequestedPayloads() = runTest {
        insertWithPayload(
            message(tid = 101),
            attachment(id = 1, messageTid = 101),
        )
        insertWithPayload(
            message(tid = 202),
            attachment(id = 2, messageTid = 202),
        )

        val result = attachmentDao.getAllAttachmentPayLoadsByMsgTid(202L)

        assertThat(result.map { it.payLoadEntity.messageTid }).containsExactly(202L)
        assertThat(result.single().payLoadEntity.filePath).isEqualTo("path_202")
    }

    @Test
    fun updateAttachmentIdAndMessageId_updatesMatchingAttachment() = runTest {
        insertAttachmentsOnly(attachment(id = 1, messageTid = 10, messageId = 10, url = "old_url"))

        attachmentDao.updateAttachmentIdAndMessageId(
            attachmentId = 99L,
            messageId = 55L,
            messageTid = 10L,
            attachmentUrl = "old_url",
        )

        val updated = getAttachment(99L)
        assertThat(updated.id).isEqualTo(99L)
        assertThat(updated.messageId).isEqualTo(55L)
    }

    @Test
    fun updateAttachmentTransferDataByMsgTid_updatesPayloadStateAndProgress() = runTest {
        insertWithPayload(
            message(tid = 11),
            attachment(id = 1, messageTid = 11),
        )

        attachmentDao.updateAttachmentTransferDataByMsgTid(11L, 0.75f, TransferState.Downloading)

        val payload = attachmentDao.getAllAttachmentPayLoadsByMsgTid(11L).single().payLoadEntity
        assertThat(payload.progressPercent).isEqualTo(0.75f)
        assertThat(payload.transferState).isEqualTo(TransferState.Downloading)
    }

    @Test
    fun updateAttachmentAndPayLoad_updatesBothTablesForRegularAttachment() = runTest {
        insertWithPayload(
            message(tid = 12),
            attachment(id = 2, messageTid = 12, url = "old_url", filePath = "old_path"),
        )

        attachmentDao.updateAttachmentAndPayLoad(
            TransferData(
                messageTid = 12L,
                progressPercent = 0.5f,
                state = TransferState.Uploading,
                filePath = "new_path",
                url = "new_url",
            )
        )

        val updatedAttachment = getAttachment(2L)
        val payload = attachmentDao.getAllAttachmentPayLoadsByMsgTid(12L).single().payLoadEntity

        assertThat(updatedAttachment.filePath).isEqualTo("new_path")
        assertThat(updatedAttachment.url).isEqualTo("new_url")
        assertThat(payload.filePath).isEqualTo("new_path")
        assertThat(payload.url).isEqualTo("new_url")
        assertThat(payload.progressPercent).isEqualTo(0.5f)
        assertThat(payload.transferState).isEqualTo(TransferState.Uploading)
    }

    @Test
    fun updateAttachmentFilePathAndMetadata_updatesAttachmentAndPayloadFilePath() = runTest {
        insertWithPayload(
            message(tid = 13),
            attachment(id = 3, messageTid = 13, filePath = "old_path", metadata = "old_meta", fileSize = 10L),
        )

        attachmentDao.updateAttachmentFilePathAndMetadata(
            tid = 13L,
            filePath = "final_path",
            fileSize = 500L,
            metadata = "new_meta",
        )

        val updatedAttachment = getAttachment(3L)
        val payload = attachmentDao.getAllAttachmentPayLoadsByMsgTid(13L).single().payLoadEntity

        assertThat(updatedAttachment.filePath).isEqualTo("final_path")
        assertThat(updatedAttachment.fileSize).isEqualTo(500L)
        assertThat(updatedAttachment.metadata).isEqualTo("new_meta")
        assertThat(payload.filePath).isEqualTo("final_path")
    }

    @Test
    fun updateAttachmentByMsgTid_doesNotModifyLinkAttachments() = runTest {
        insertAttachmentsOnly(
            attachment(
                id = 4,
                messageTid = 14,
                type = AttachmentTypeEnum.Link.value,
                url = "link_url",
                filePath = null,
            )
        )

        attachmentDao.updateAttachmentByMsgTid(
            msgTid = 14L,
            filePath = "ignored_path",
            url = "ignored_url",
        )

        val updated = getAttachment(4L, type = AttachmentTypeEnum.Link.value)
        assertThat(updated.url).isEqualTo("link_url")
        assertThat(updated.filePath).isNull()
    }

    @Test
    fun updateAttachmentFilePathByMsgTid_doesNotModifyLinkAttachments() = runTest {
        insertAttachmentsOnly(
            attachment(
                id = 5,
                messageTid = 15,
                type = AttachmentTypeEnum.Link.value,
                filePath = null,
                metadata = "old_meta",
                fileSize = 20L,
            )
        )

        attachmentDao.updateAttachmentFilePathByMsgTid(
            msgTid = 15L,
            filePath = "ignored_path",
            fileSize = 999L,
            metadata = "new_meta",
        )

        val updated = getAttachment(5L, type = AttachmentTypeEnum.Link.value)
        assertThat(updated.filePath).isNull()
        assertThat(updated.fileSize).isEqualTo(20L)
        assertThat(updated.metadata).isEqualTo("old_meta")
    }
}
