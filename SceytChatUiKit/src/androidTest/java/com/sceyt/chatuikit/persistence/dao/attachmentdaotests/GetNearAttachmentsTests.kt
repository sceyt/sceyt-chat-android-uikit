package com.sceyt.chatuikit.persistence.dao.attachmentdaotests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetNearAttachmentsTests {
    private lateinit var database: SceytDatabase
    private lateinit var attachmentDao: AttachmentDao
    private val channelId = 1L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SceytDatabase::class.java)
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        attachmentDao = database.attachmentsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createAttachment(id: Long, messageTid: Long, type: String = AttachmentTypeEnum.Image.value): AttachmentEntity {
        return AttachmentEntity(
            id = id,
            messageId = id,
            messageTid = messageTid,
            channelId = channelId,
            userId = "1",
            name = "attachment_$id",
            type = type,
            metadata = null,
            fileSize = 100L,
            createdAt = id,
            url = "url_$id",
            filePath = "path_$id",
            originalFilePath = null
        )
    }

    // Helper method to create and add a message entity
    private suspend fun addMessageEntity(tid: Long) {
        val message = MessageEntity(
            tid = tid,
            id = tid,
            channelId = channelId,
            body = "Test message $tid",
            type = "text",
            bodyAttribute = null,
            createdAt = System.currentTimeMillis(),
            incoming = false,
            isTransient = false,
            silent = false,
            deliveryStatus = DeliveryStatus.Displayed,
            state = MessageState.Unmodified,
            fromId = "1",
            markerCount = null,
            mentionedUsersIds = null,
            parentId = null,
            replyCount = 0,
            displayCount = 0,
            autoDeleteAt = null,
            forwardingDetailsDb = null,
            metadata = "",
            updatedAt = 0,
            unList = false,
            disableMentionsCount = false
        )
        database.messageDao().upsertMessageEntitiesWithTransaction(listOf(message))
    }

    // Helper method to insert attachments correctly
    private suspend fun insertAttachments(attachments: List<AttachmentEntity>) {
        // First insert the required message entities
        for (attachment in attachments) {
            addMessageEntity(attachment.messageTid)
        }

        attachmentDao.insertAttachments(attachments)
    }

    @Test
    fun getNearAttachmentsShouldReturnAttachmentsFromMiddle() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Image.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value),
            createAttachment(4, 104, AttachmentTypeEnum.Image.value),
            createAttachment(5, 105, AttachmentTypeEnum.Image.value),
            createAttachment(6, 106, AttachmentTypeEnum.Image.value),
            createAttachment(7, 107, AttachmentTypeEnum.Image.value),
            createAttachment(8, 108, AttachmentTypeEnum.Image.value),
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 6
        val result = attachmentDao.getNearAttachments(
            channelId,
            4, // Load from the middle
            limit,
            listOf(AttachmentTypeEnum.Image.value)
        )

        val loadedAttachments = result.data
        val attachmentIds = loadedAttachments.map { it.attachmentEntity.id }

        // Then
        Log.d("loadedAttachments", "$attachmentIds")
        // Should return attachments before and after the middle point
        Truth.assertThat(attachmentIds).contains(4L) // Should contain the target attachment
        Truth.assertThat(attachmentIds.size).isAtMost(limit)
        Truth.assertThat(attachmentIds.first()).isLessThan(4L) // Should have at least one attachment before middle
        Truth.assertThat(attachmentIds.last()).isGreaterThan(4L) // Should have at least one attachment after middle
    }

    @Test
    fun getNearAttachments_HasNotNextIfNewestSizeNotBiggerHalfLimit() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Image.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value),
            createAttachment(4, 104, AttachmentTypeEnum.Image.value),
            createAttachment(5, 105, AttachmentTypeEnum.Image.value)
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 4
        val result = attachmentDao.getNearAttachments(
            channelId,
            3,
            limit,
            listOf(AttachmentTypeEnum.Image.value)
        )

        val loadedAttachments = result.data
        val attachmentIds = loadedAttachments.map { it.attachmentEntity.id }

        // Then
        Log.d("loadedAttachments", "$attachmentIds")
        Truth.assertThat(loadedAttachments.size).isEqualTo(4)
        // Since there are only 2 newer attachments (4,5) which is less than half limit (2),
        // hasNext should be false
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun getNearAttachmentsShouldReturnAttachmentsOfSpecifiedType() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Video.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value),
            createAttachment(4, 104, AttachmentTypeEnum.File.value),
            createAttachment(5, 105, AttachmentTypeEnum.Image.value),
            createAttachment(6, 106, AttachmentTypeEnum.Voice.value),
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 10
        val result = attachmentDao.getNearAttachments(
            channelId,
            3,
            limit,
            listOf(AttachmentTypeEnum.Image.value)
        )

        val loadedAttachments = result.data
        val attachmentIds = loadedAttachments.map { it.attachmentEntity.id }

        // Then
        Log.d("loadedAttachments", "$attachmentIds")
        Truth.assertThat(attachmentIds).isEqualTo(listOf(1L, 3L, 5L))
        Truth.assertThat(loadedAttachments.size).isEqualTo(3)
        Truth.assertThat(loadedAttachments.all { it.attachmentEntity.type == AttachmentTypeEnum.Image.value }).isTrue()
    }

    @Test
    fun getNearAttachmentsShouldReturnEmptyIfAttachmentNotFound() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Video.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value)
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 10
        val result = attachmentDao.getNearAttachments(
            channelId,
            10, // Non-existent attachment ID
            limit,
            listOf(AttachmentTypeEnum.Image.value, AttachmentTypeEnum.Video.value)
        )

        // Then
        Truth.assertThat(result.data).isEmpty()
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun getNearAttachments_ShouldHaveNextIfNewestSizeEqualHalfLimit() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Image.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value),
            createAttachment(4, 104, AttachmentTypeEnum.Image.value),
            createAttachment(5, 105, AttachmentTypeEnum.Image.value),
            createAttachment(6, 106, AttachmentTypeEnum.Image.value),
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 4
        val result = attachmentDao.getNearAttachments(
            channelId,
            3,
            limit,
            listOf(AttachmentTypeEnum.Image.value)
        )

        val loadedAttachments = result.data
        val attachmentIds = loadedAttachments.map { it.attachmentEntity.id }

        // Then
        Log.d("loadedAttachments", "$attachmentIds")
        Truth.assertThat(loadedAttachments.size).isEqualTo(4)
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun getNearAttachments_ShouldHavePrevIfOldestSizeEqualHalfLimit() = runTest {
        // Given
        val attachments = listOf(
            createAttachment(1, 101, AttachmentTypeEnum.Image.value),
            createAttachment(2, 102, AttachmentTypeEnum.Image.value),
            createAttachment(3, 103, AttachmentTypeEnum.Image.value),
            createAttachment(4, 104, AttachmentTypeEnum.Image.value),
            createAttachment(5, 105, AttachmentTypeEnum.Image.value),
            createAttachment(6, 106, AttachmentTypeEnum.Image.value),
        )

        // Insert attachments
        insertAttachments(attachments)

        // When
        val limit = 4
        val result = attachmentDao.getNearAttachments(
            channelId,
            4,
            limit,
            listOf(AttachmentTypeEnum.Image.value)
        )

        val loadedAttachments = result.data
        val attachmentIds = loadedAttachments.map { it.attachmentEntity.id }

        // Then
        Log.d("loadedAttachments", "$attachmentIds")
        Truth.assertThat(loadedAttachments.size).isEqualTo(4)
        Truth.assertThat(result.hasPrev).isTrue()
    }
}