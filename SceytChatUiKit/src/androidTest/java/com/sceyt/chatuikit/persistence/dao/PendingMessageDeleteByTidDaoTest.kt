package com.sceyt.chatuikit.persistence.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageDeleteByTidDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageDeleteByTidEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PendingMessageDeleteByTidDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var dao: PendingMessageDeleteByTidDao
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
        dao = database.pendingMessageDeleteByTidDao()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        tid: Long,
        channelId: Long = 1L,
        deleteType: DeleteMessageType = DeleteMessageType.DeleteForEveryone,
        createdAt: Long = tid,
    ) = PendingMessageDeleteByTidEntity(
        messageTid = tid,
        channelId = channelId,
        deleteType = deleteType,
        createdAt = createdAt,
    )

    private fun message(tid: Long, channelId: Long = 1L) = MessageDb(
        messageEntity = MessageEntity(
            tid = tid,
            id = tid,
            channelId = channelId,
            body = "body",
            type = "text",
            metadata = null,
            createdAt = tid,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            deliveryStatus = MessageDeliveryStatus.Pending,
            state = MessageState.Unmodified,
            fromId = "user1",
            markerCount = null,
            mentionedUsersIds = null,
            parentId = null,
            replyCount = 0L,
            displayCount = 0,
            autoDeleteAt = null,
            forwardingDetailsDb = null,
            bodyAttribute = null,
            unList = false,
            disableMentionsCount = false,
            viewOnce = false,
        ),
        from = null,
        parent = null,
        attachments = null,
        userMarkers = null,
        reactions = null,
        reactionsTotals = null,
        pendingReactions = null,
        forwardingUser = null,
        mentionedUsers = null,
        poll = null,
    )

    @Test
    fun insert_and_getAll_returnsStoredEntity() = runTest {
        dao.insert(entity(tid = 1, deleteType = DeleteMessageType.DeleteHard))

        val result = dao.getAll()

        assertThat(result).hasSize(1)
        assertThat(result.first().messageTid).isEqualTo(1L)
        assertThat(result.first().deleteType).isEqualTo(DeleteMessageType.DeleteHard)
    }

    @Test
    fun insert_replacesOnConflictByTid() = runTest {
        dao.insert(entity(tid = 1, deleteType = DeleteMessageType.DeleteForMe))
        dao.insert(entity(tid = 1, deleteType = DeleteMessageType.DeleteForEveryone))

        val result = dao.getAll()

        assertThat(result).hasSize(1)
        assertThat(result.first().deleteType).isEqualTo(DeleteMessageType.DeleteForEveryone)
    }

    @Test
    fun getAll_orderedByCreatedAtAsc() = runTest {
        dao.insert(entity(tid = 1, createdAt = 300))
        dao.insert(entity(tid = 2, createdAt = 100))
        dao.insert(entity(tid = 3, createdAt = 200))

        assertThat(dao.getAll().map { it.messageTid }).isEqualTo(listOf(2L, 3L, 1L))
    }

    @Test
    fun deleteByTid_removesOnlyThatRow() = runTest {
        dao.insert(entity(tid = 1))
        dao.insert(entity(tid = 2))

        dao.deleteByTid(1L)

        assertThat(dao.getAll().map { it.messageTid }).containsExactly(2L)
    }

    @Test
    fun updateChannelId_remapsMatchingRows() = runTest {
        dao.insert(entity(tid = 1, channelId = 10L))
        dao.insert(entity(tid = 2, channelId = 10L))
        dao.insert(entity(tid = 3, channelId = 20L))

        val updated = dao.updateChannelId(oldChannelId = 10L, newChannelId = 99L)

        assertThat(updated).isEqualTo(2)
        assertThat(dao.getAll().filter { it.channelId == 99L }.map { it.messageTid })
            .containsExactlyElementsIn(listOf(1L, 2L))
        assertThat(dao.getAll().first { it.messageTid == 3L }.channelId).isEqualTo(20L)
    }

    // Core invariant: the pending-delete row must survive deletion of the message row
    // (no foreign key / cascade), so the delete can be retried after the message is gone.
    @Test
    fun pendingRowSurvivesMessageDeletion() = runTest {
        messageDao.upsertMessage(message(tid = 1))
        dao.insert(entity(tid = 1))

        messageDao.deleteMessageByTid(1L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(dao.getAll().map { it.messageTid }).containsExactly(1L)
    }
}