package com.sceyt.chatuikit.persistence.dao.messagedaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MessageDaoTest {

    private lateinit var database: SceytDatabase
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
        createdAt: Long = id,
        deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Displayed,
        incoming: Boolean = false,
        unList: Boolean = false,
        autoDeleteAt: Long? = null,
    ) = MessageEntity(
        tid = tid,
        id = id,
        channelId = channelId,
        body = "body",
        type = "text",
        metadata = null,
        createdAt = createdAt,
        updatedAt = 0,
        incoming = incoming,
        isTransient = false,
        silent = false,
        deliveryStatus = deliveryStatus,
        state = MessageState.Unmodified,
        fromId = "user1",
        markerCount = null,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0L,
        displayCount = 0,
        autoDeleteAt = autoDeleteAt,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        unList = unList,
        disableMentionsCount = false,
        viewOnce = false,
    )

    private fun messageDb(entity: MessageEntity) = MessageDb(
        messageEntity = entity,
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

    private suspend fun insert(vararg entities: MessageEntity) {
        messageDao.upsertMessageEntitiesWithTransaction(entities.toList())
    }

    // endregion

    // region upsert behavior

    @Test
    fun upsertMessage_insertsNewMessage() = runTest {
        messageDao.upsertMessage(messageDb(message(tid = 1, id = 10)))

        assertThat(messageDao.getMessageById(10L)).isNotNull()
    }

    @Test
    fun upsertMessage_updatesExistingMessage() = runTest {
        messageDao.upsertMessage(messageDb(message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent)))

        messageDao.upsertMessage(messageDb(message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Displayed)))

        val result = messageDao.getMessageById(10L)!!.messageEntity
        assertThat(result.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
    }

    @Test
    fun insertMessagesIgnored_doesNotOverwriteExistingMessage() = runTest {
        messageDao.upsertMessage(messageDb(message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent)))

        messageDao.insertMessagesIgnored(listOf(messageDb(message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Displayed))))

        val result = messageDao.getMessageById(10L)!!.messageEntity
        assertThat(result.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
    }

    // endregion

    // region query by id / tid

    @Test
    fun getMessageById_returnsCorrectMessage() = runTest {
        insert(message(tid = 1, id = 10), message(tid = 2, id = 20))

        assertThat(messageDao.getMessageById(10L)!!.messageEntity.id).isEqualTo(10L)
    }

    @Test
    fun getMessageById_returnsNullForMissingId() = runTest {
        assertThat(messageDao.getMessageById(999L)).isNull()
    }

    @Test
    fun getMessageByTid_returnsCorrectMessage() = runTest {
        insert(message(tid = 42, id = 100))

        assertThat(messageDao.getMessageByTid(42L)!!.messageEntity.tid).isEqualTo(42L)
    }

    @Test
    fun getMessageByTid_returnsNullForMissingTid() = runTest {
        assertThat(messageDao.getMessageByTid(999L)).isNull()
    }

    @Test
    fun getMessagesByTid_returnsOnlyMatchingMessages() = runTest {
        insert(message(tid = 1), message(tid = 2), message(tid = 3))

        val result = messageDao.getMessagesByTid(listOf(1L, 3L))

        assertThat(result.map { it.messageEntity.tid }).containsExactlyElementsIn(listOf(1L, 3L))
    }

    @Test
    fun getMessageTidById_returnsCorrectTid() = runTest {
        insert(message(tid = 77, id = 100))

        assertThat(messageDao.getMessageTidById(100L)).isEqualTo(77L)
    }

    @Test
    fun getMessageTidById_returnsNullForMissingId() = runTest {
        assertThat(messageDao.getMessageTidById(999L)).isNull()
    }

    @Test
    fun getMessageTIdsByIds_returnsCorrectTids() = runTest {
        insert(message(tid = 10, id = 1), message(tid = 20, id = 2), message(tid = 30, id = 3))

        val result = messageDao.getMessageTIdsByIds(1L, 3L)

        assertThat(result).containsExactlyElementsIn(listOf(10L, 30L))
    }

    @Test
    fun getExistMessageByIds_returnsOnlyPresentIds() = runTest {
        insert(message(tid = 1, id = 10), message(tid = 2, id = 20))

        val result = messageDao.getExistMessageByIds(listOf(10L, 20L, 999L))

        assertThat(result).containsExactlyElementsIn(listOf(10L, 20L))
    }

    @Test
    fun getExistMessagesIdTidByIds_returnsPairsForPresentIds() = runTest {
        insert(message(tid = 5, id = 100), message(tid = 6, id = 200))

        val result = messageDao.getExistMessagesIdTidByIds(listOf(100L, 200L, 999L))

        assertThat(result.map { it.id }).containsExactlyElementsIn(listOf(100L, 200L))
        assertThat(result.map { it.tid }).containsExactlyElementsIn(listOf(5L, 6L))
    }

    @Test
    fun existsMessageById_returnsTrueWhenPresent() = runTest {
        insert(message(tid = 1, id = 10))

        assertThat(messageDao.existsMessageById(10L)).isTrue()
    }

    @Test
    fun existsMessageById_returnsFalseWhenAbsent() = runTest {
        assertThat(messageDao.existsMessageById(999L)).isFalse()
    }

    @Test
    fun existsMessageByTid_returnsTrueWhenPresent() = runTest {
        insert(message(tid = 42, id = 1))

        assertThat(messageDao.existsMessageByTid(42L)).isTrue()
    }

    @Test
    fun existsMessageByTid_returnsFalseWhenAbsent() = runTest {
        assertThat(messageDao.existsMessageByTid(999L)).isFalse()
    }

    // endregion

    // region pending messages

    @Test
    fun getPendingMessages_returnsOnlyPendingForChannel() = runTest {
        insert(
            message(tid = 1, id = 1, channelId = 1L, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 2, id = 2, channelId = 1L, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 3, id = 3, channelId = 2L, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        val result = messageDao.getPendingMessages(channelId = 1L)

        assertThat(result.map { it.messageEntity.tid }).containsExactly(1L)
    }

    @Test
    fun getPendingMessages_orderedByCreatedAt() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 300, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 2, id = 2, createdAt = 100, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 3, id = 3, createdAt = 200, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        val result = messageDao.getPendingMessages(channelId = 1L)

        assertThat(result.map { it.messageEntity.tid }).isEqualTo(listOf(2L, 3L, 1L))
    }

    @Test
    fun getAllPendingMessages_returnsAcrossAllChannels() = runTest {
        insert(
            message(tid = 1, id = 1, channelId = 1L, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 2, id = 2, channelId = 2L, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 3, id = 3, channelId = 1L, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        val result = messageDao.getAllPendingMessages()

        assertThat(result.map { it.messageEntity.tid }).containsExactlyElementsIn(listOf(1L, 2L))
    }

    @Test
    fun getPendingMessageByTid_returnsNullForNonPendingMessage() = runTest {
        insert(message(tid = 5, id = 5, deliveryStatus = MessageDeliveryStatus.Sent))

        assertThat(messageDao.getPendingMessageByTid(5L)).isNull()
    }

    @Test
    fun getPendingMessagesByTIds_returnsOnlyPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 1, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 2, id = 2, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        val result = messageDao.getPendingMessagesByTIds(listOf(1L, 2L))

        assertThat(result.map { it.messageEntity.tid }).containsExactly(1L)
    }

    // endregion

    // region last message / counts

    @Test
    fun getLastMessage_returnsMessageWithHighestCreatedAt() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 100),
            message(tid = 2, id = 2, createdAt = 300),
            message(tid = 3, id = 3, createdAt = 200),
        )

        val result = messageDao.getLastMessage(channelId = 1L)

        assertThat(result!!.messageEntity.tid).isEqualTo(2L)
    }

    @Test
    fun getLastSentMessageId_excludesPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        assertThat(messageDao.getLastSentMessageId(channelId = 1L)).isEqualTo(10L)
    }

    @Test
    fun getLastSentMessageId_returnsNullWhenAllPending() = runTest {
        insert(message(tid = 1, id = 1, deliveryStatus = MessageDeliveryStatus.Pending))

        assertThat(messageDao.getLastSentMessageId(channelId = 1L)).isNull()
    }

    @Test
    fun getMessagesCount_returnsCorrectCount() = runTest {
        insert(message(tid = 1), message(tid = 2), message(tid = 3))

        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(3)
    }

    @Test
    fun getMessagesCountAsFlow_reflectsUpdatedCountAfterInsert() = runTest {
        val flow = messageDao.getMessagesCountAsFlow(channelId = 1L)

        assertThat(flow.first()).isEqualTo(0L)

        insert(message(tid = 1))

        assertThat(flow.first()).isEqualTo(1L)
    }

    @Test
    fun getMessagesIds_returnsIdsOrderedByCreatedAt() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 300),
            message(tid = 2, id = 20, createdAt = 100),
            message(tid = 3, id = 30, createdAt = 200),
        )

        val result = messageDao.getMessagesIds(channelId = 1L)

        assertThat(result).isEqualTo(listOf(20L, 30L, 10L))
    }

    // endregion

    // region messages in range

    @Test
    fun getMessagesIdsByRangeIgnoreUnlist_returnsIdsInRange() = runTest {
        insert(
            message(tid = 1, id = 10),
            message(tid = 2, id = 20),
            message(tid = 3, id = 30),
        )

        val result = messageDao.getMessagesIdsByRangeIgnoreUnlist(
            channelId = 1L, startId = 10L, endId = 25L,
        )

        assertThat(result).containsExactlyElementsIn(listOf(10L, 20L))
    }

    @Test
    fun getMessagesIdsByRangeIgnoreUnlist_excludesPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        val result = messageDao.getMessagesIdsByRangeIgnoreUnlist(
            channelId = 1L, startId = 1L, endId = 100L,
        )

        assertThat(result).containsExactly(10L)
    }

    @Test
    fun getMessagesIdsByRangeIgnoreUnlist_excludesUnlistedMessages() = runTest {
        insert(
            message(tid = 1, id = 10, unList = false),
            message(tid = 2, id = 20, unList = true),
        )

        val result = messageDao.getMessagesIdsByRangeIgnoreUnlist(
            channelId = 1L, startId = 1L, endId = 100L,
        )

        assertThat(result).containsExactly(10L)
    }

    @Test
    fun getMessagesIdsByRangeIgnoreUnlist_zeroDateUntilReturnsAll() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200),
        )

        // dateUntil = 0 means no date filter
        val result = messageDao.getMessagesIdsByRangeIgnoreUnlist(
            channelId = 1L, startId = 1L, endId = 100L, dateUntil = 0L,
        )

        assertThat(result).containsExactlyElementsIn(listOf(10L, 20L))
    }

    @Test
    fun getMessagesIdsByRangeIgnoreUnlist_dateUntilFiltersResults() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200),
            message(tid = 3, id = 30, createdAt = 300),
        )

        // dateUntil = 250 → only messages with createdAt < 250
        val result = messageDao.getMessagesIdsByRangeIgnoreUnlist(
            channelId = 1L, startId = 1L, endId = 100L, dateUntil = 250L,
        )

        assertThat(result).containsExactlyElementsIn(listOf(10L, 20L))
    }

    // endregion

    // region delivery status queries

    @Test
    fun getMessagesTidAndIdLoverThanByStatus_returnsMatchingMessages() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Received),
            message(tid = 3, id = 30, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        // id <= 25 and status = Sent → only message id=10 qualifies
        val result = messageDao.getMessagesTidAndIdLoverThanByStatus(
            channelId = 1L, id = 25L, MessageDeliveryStatus.Sent,
        )

        assertThat(result.map { it.id }).containsExactly(10L)
    }

    @Test
    fun getMessagesTidAndIdLoverThanByStatus_supportsMultipleStatuses() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Received),
            message(tid = 3, id = 30, deliveryStatus = MessageDeliveryStatus.Displayed),
        )

        val result = messageDao.getMessagesTidAndIdLoverThanByStatus(
            channelId = 1L, id = 100L, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Received,
        )

        assertThat(result.map { it.id }).containsExactlyElementsIn(listOf(10L, 20L))
    }

    // endregion

    // region update delivery status

    @Test
    fun updateMessageStatus_updatesSpecifiedMessages() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        messageDao.updateMessageStatus(MessageDeliveryStatus.Displayed, 10L)

        assertThat(messageDao.getMessageById(10L)!!.messageEntity.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Displayed)
        // Message 20 unchanged
        assertThat(messageDao.getMessageById(20L)!!.messageEntity.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Sent)
    }

    @Test
    fun updateAllIncomingMessagesStatusAsRead_updatesOnlyIncomingMessages() = runTest {
        insert(
            message(tid = 1, id = 10, incoming = true, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, incoming = false, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        messageDao.updateAllIncomingMessagesStatusAsRead(channelId = 1L)

        assertThat(messageDao.getMessageById(10L)!!.messageEntity.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Displayed)
        assertThat(messageDao.getMessageById(20L)!!.messageEntity.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Sent)
    }

    @Test
    fun updateMessagesChannelId_movesAllMessagesToNewChannel() = runTest {
        insert(
            message(tid = 1, id = 1, channelId = 1L),
            message(tid = 2, id = 2, channelId = 1L),
            message(tid = 3, id = 3, channelId = 2L),
        )

        val updated = messageDao.updateMessagesChannelId(oldChannelId = 1L, newChannelId = 99L)

        assertThat(updated).isEqualTo(2)
        assertThat(messageDao.getMessagesCount(channelId = 99L)).isEqualTo(2)
        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(0)
        // Channel 2 messages are untouched
        assertThat(messageDao.getMessagesCount(channelId = 2L)).isEqualTo(1)
    }

    @Test
    fun updateMessageStatusWithBefore_marksAllSentAndReceivedAsDisplayed() = runTest {
        insert(
            message(tid = 1, id = 10, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 20, deliveryStatus = MessageDeliveryStatus.Received),
            message(tid = 3, id = 30, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 4, id = 40, deliveryStatus = MessageDeliveryStatus.Sent),
        )

        // Mark all messages up to id=30 as Displayed
        val updated = messageDao.updateMessageStatusWithBefore(
            channelId = 1L, status = MessageDeliveryStatus.Displayed, id = 30L,
        )

        assertThat(updated.map { it.id }).containsExactlyElementsIn(listOf(10L, 20L, 30L))
        // Message 40 (id > 30) should be unchanged
        assertThat(messageDao.getMessageById(40L)!!.messageEntity.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Sent)
    }

    // endregion

    // region delete queries

    @Test
    fun deleteMessageByTid_removesOnlyThatMessage() = runTest {
        insert(message(tid = 1, id = 1), message(tid = 2, id = 2))

        messageDao.deleteMessageByTid(1L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isTrue()
    }

    @Test
    fun deleteMessagesByTid_removesMultipleAndReturnsCount() = runTest {
        insert(message(tid = 1, id = 1), message(tid = 2, id = 2), message(tid = 3, id = 3))

        val deleted = messageDao.deleteMessagesByTid(listOf(1L, 2L))

        assertThat(deleted).isEqualTo(2)
        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(1)
    }

    @Test
    fun deleteAllMessagesByChannel_removesOnlyThatChannel() = runTest {
        insert(
            message(tid = 1, id = 1, channelId = 1L),
            message(tid = 2, id = 2, channelId = 2L),
        )

        messageDao.deleteAllMessagesByChannel(channelId = 1L)

        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(0)
        assertThat(messageDao.getMessagesCount(channelId = 2L)).isEqualTo(1)
    }

    @Test
    fun deleteAllChannelsMessages_removesMessagesForAllGivenChannels() = runTest {
        insert(
            message(tid = 1, id = 1, channelId = 1L),
            message(tid = 2, id = 2, channelId = 2L),
            message(tid = 3, id = 3, channelId = 3L),
        )

        messageDao.deleteAllChannelsMessages(channelIds = listOf(1L, 2L))

        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(0)
        assertThat(messageDao.getMessagesCount(channelId = 2L)).isEqualTo(0)
        assertThat(messageDao.getMessagesCount(channelId = 3L)).isEqualTo(1)
    }

    @Test
    fun deleteUntilDateExceptPending_zeroDeletesAllNonPending() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 100, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 2, createdAt = 200, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        messageDao.deleteUntilDateExceptPending(channelId = 1L, deleteUntil = 0L)

        // Only pending remains
        assertThat(messageDao.getMessagesCount(channelId = 1L)).isEqualTo(1)
        assertThat(messageDao.existsMessageByTid(2L)).isTrue()
    }

    @Test
    fun deleteUntilDateExceptPending_deletesOnlyStrictlyBeforeDate() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 100),
            message(tid = 2, id = 2, createdAt = 200),
            message(tid = 3, id = 3, createdAt = 300),
        )

        // deleteUntil = 250 → removes messages with createdAt < 250
        messageDao.deleteUntilDateExceptPending(channelId = 1L, deleteUntil = 250L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isFalse()
        assertThat(messageDao.existsMessageByTid(3L)).isTrue()
    }

    @Test
    fun deleteMessagesBeforeDateExceptPending_deletesMessagesAtOrBeforeDate() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 100),
            message(tid = 2, id = 2, createdAt = 200),
            message(tid = 3, id = 3, createdAt = 300),
        )

        // Query uses createdAt <= date, so 200 is included
        messageDao.deleteMessagesBeforeDateExceptPending(channelId = 1L, date = 200L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isFalse()
        assertThat(messageDao.existsMessageByTid(3L)).isTrue()
    }

    @Test
    fun deleteMessagesBeforeDateExceptPending_keepsPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 1, createdAt = 100, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 2, id = 2, createdAt = 50),
        )

        messageDao.deleteMessagesBeforeDateExceptPending(channelId = 1L, date = 200L)

        assertThat(messageDao.existsMessageByTid(1L)).isTrue()
        assertThat(messageDao.existsMessageByTid(2L)).isFalse()
    }

    @Test
    fun deleteMessagesBeforeIdExceptPending_deletesNonPendingAtOrBeforeId() = runTest {
        insert(
            message(tid = 1, id = 10),
            message(tid = 2, id = 20),
            message(tid = 3, id = 30),
            message(tid = 4, id = 5, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        messageDao.deleteMessagesBeforeIdExceptPending(channelId = 1L, messageId = 20L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse() // id 10 <= 20
        assertThat(messageDao.existsMessageByTid(2L)).isFalse() // id 20 <= 20
        assertThat(messageDao.existsMessageByTid(3L)).isTrue()  // id 30 > 20
        assertThat(messageDao.existsMessageByTid(4L)).isTrue()  // pending, spared
    }

    @Test
    fun deleteMessagesAfterIdUntilDateExceptPending_zeroDeletesAllAfterIdExceptPending() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200),
            message(tid = 3, id = 5, createdAt = 50, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        // deleteUntil = 0 means no date filter — delete all non-pending with id >= 10
        messageDao.deleteMessagesAfterIdUntilDateExceptPending(
            channelId = 1L, messageId = 10L, deleteUntil = 0L,
        )

        assertThat(messageDao.existsMessageByTid(1L)).isFalse() // id 10 >= 10
        assertThat(messageDao.existsMessageByTid(2L)).isFalse() // id 20 >= 10
        assertThat(messageDao.existsMessageByTid(3L)).isTrue()  // pending, spared
    }

    @Test
    fun deleteMessagesAfterIdUntilDateExceptPending_dateFilterLimitsDelete() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200),
            message(tid = 3, id = 30, createdAt = 300),
        )

        // Delete messages with id >= 10 and createdAt < 250
        messageDao.deleteMessagesAfterIdUntilDateExceptPending(
            channelId = 1L, messageId = 10L, deleteUntil = 250L,
        )

        assertThat(messageDao.existsMessageByTid(1L)).isFalse() // createdAt 100 < 250
        assertThat(messageDao.existsMessageByTid(2L)).isFalse() // createdAt 200 < 250
        assertThat(messageDao.existsMessageByTid(3L)).isTrue()  // createdAt 300 >= 250
    }

    @Test
    fun deleteAllMessagesExceptPending_keepsPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 1, deliveryStatus = MessageDeliveryStatus.Sent),
            message(tid = 2, id = 2, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        messageDao.deleteAllMessagesExceptPending(channelId = 1L)

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isTrue()
    }

    @Test
    fun deleteNotInMessageIdsUntilDateExceptPending_keepsListedMessages() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200),
            message(tid = 3, id = 30, createdAt = 300),
        )

        // Keep message 20, delete the rest (no date filter)
        messageDao.deleteNotInMessageIdsUntilDateExceptPending(
            channelId = 1L,
            messageIds = listOf(20L),
            deleteUntil = 0L,
        )

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isTrue()
        assertThat(messageDao.existsMessageByTid(3L)).isFalse()
    }

    @Test
    fun deleteNotInMessageIdsUntilDateExceptPending_keepsPendingMessages() = runTest {
        insert(
            message(tid = 1, id = 10, createdAt = 100),
            message(tid = 2, id = 20, createdAt = 200, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        // Message 20 is pending — it should survive even though it's not in the keep-list
        messageDao.deleteNotInMessageIdsUntilDateExceptPending(
            channelId = 1L,
            messageIds = emptyList(),
            deleteUntil = 0L,
        )

        assertThat(messageDao.existsMessageByTid(1L)).isFalse()
        assertThat(messageDao.existsMessageByTid(2L)).isTrue()
    }

    // endregion

    // region auto-delete messages

    @Test
    fun getOutdatedMessageTIds_returnsExpiredAutoDeleteMessages() = runTest {
        // autoDeleteAt is stored in a separate table via upsertMessage
        messageDao.upsertMessage(messageDb(message(tid = 1, id = 1, autoDeleteAt = 1000L)))
        messageDao.upsertMessage(messageDb(message(tid = 2, id = 2, autoDeleteAt = 9999L)))

        // localTime = 5000 → only tid=1 (autoDeleteAt=1000) is expired
        val result = messageDao.getOutdatedMessageTIds(channelId = 1L, localTime = 5000L)

        assertThat(result).containsExactly(1L)
    }

    @Test
    fun getOutdatedMessageTIds_returnsEmptyWhenNoAutoDeleteMessages() = runTest {
        insert(message(tid = 1, id = 1))

        val result = messageDao.getOutdatedMessageTIds(channelId = 1L, localTime = 9999L)

        assertThat(result).isEmpty()
    }

    // endregion
}