package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreviousPendingMessageTidsTest {
    private lateinit var database: SceytDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SceytDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `messages created in the same millisecond follow save order whatever their tids are`() = runBlocking<Unit> {
        val messageDao = database.messageDao()
        for (tid in listOf(FIRST_SAVED_TID, SECOND_SAVED_TID, THIRD_SAVED_TID)) {
            val message = createMessage(createdAt = CREATED_AT, tid = tid).copy(
                deliveryStatus = MessageDeliveryStatus.Pending,
                attachments = listOf(file(tid)),
            )
            messageDao.upsertMessage(message.toMessageDb(unList = false))
        }

        assertThat(messageDao.getPreviousPendingMessageTids(FIRST_SAVED_TID)).isEmpty()
        assertThat(messageDao.getPreviousPendingMessageTids(SECOND_SAVED_TID))
            .containsExactly(FIRST_SAVED_TID)
        assertThat(messageDao.getPreviousPendingMessageTids(THIRD_SAVED_TID))
            .containsExactly(FIRST_SAVED_TID, SECOND_SAVED_TID)
    }

    private fun file(messageTid: Long) = SceytAttachment(
        id = 0L,
        messageId = 0L,
        messageTid = messageTid,
        userId = null,
        name = "file-$messageTid",
        type = AttachmentTypeEnum.File.value,
        metadata = null,
        fileSize = 100L,
        createdAt = CREATED_AT,
        url = null,
        filePath = "/files/$messageTid",
        transferState = TransferState.PendingUpload,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private companion object {
        const val CREATED_AT = 1_000L
        const val FIRST_SAVED_TID = 30L
        const val SECOND_SAVED_TID = 20L
        const val THIRD_SAVED_TID = 10L
    }
}
