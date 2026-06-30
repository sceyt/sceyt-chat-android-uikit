package com.sceyt.chatuikit.persistence.dao.messagedaotests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetNearMessagesTests {
    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var rangeDao: LoadRangeDao
    private val channelId = 1L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SceytDatabase::class.java)
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
        rangeDao = database.loadRangeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createMessage(tid: Long, id: Long, deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Displayed): MessageEntity {
        return MessageEntity(
            tid = tid,
            id = id,
            channelId = channelId,
            body = "body",
            type = "text",
            metadata = null,
            createdAt = id,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            deliveryStatus = deliveryStatus,
            state = MessageState.Unmodified,
            fromId = "1",
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
            viewOnce = false
        )
    }

    @Test
    fun loadNearMessagesShouldReturnMessagesInRange() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(1, 4, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 6
        val result = messageDao.getNearMessages(channelId, 3, limit)
        val loadedMessages = result.data
        val messageIds = loadedMessages.map { it.messageEntity.id }

        // Then
        Log.d("loadedMessages", "$messageIds")
        Truth.assertThat(messageIds).isEqualTo(listOf(1L, 2L, 3L, 4L))
        Truth.assertThat(loadedMessages.size).isEqualTo(4)
        Truth.assertThat(loadedMessages.maxByOrNull {
            it.messageEntity.id ?: 0L
        }?.messageEntity?.id).isEqualTo(4L)
        Truth.assertThat(loadedMessages.minByOrNull {
            it.messageEntity.id ?: 0L
        }?.messageEntity?.id).isEqualTo(1L)
    }

    @Test
    fun loadNearMessagesShouldReturnMessagesInRangeCase2() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(3, 5, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 10
        val result = messageDao.getNearMessages(channelId, 5, limit)
        val loadedMessages = result.data
        val messageIds = loadedMessages.map { it.messageEntity.id }

        // Then
        Log.d("loadedMessages", "$messageIds")
        Truth.assertThat(messageIds).isEqualTo(listOf(3L, 4L, 5L))
        Truth.assertThat(loadedMessages.size).isEqualTo(3)
    }

    @Test
    fun loadNearMessagesShouldReturnEmptyIfRangeNotFoundForCurrentMessage() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)

        // When
        val result = messageDao.getNearMessages(channelId, 6, 10)

        // Then
        Truth.assertThat(result.data).isEmpty()
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessagesShouldNotReturnMessagesOutOfRange() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
            createMessage(7, 7),
            createMessage(8, 8),
            createMessage(9, 9),
            createMessage(10, 10),
        )

        val range = listOf(
            LoadRangeEntity(1, 4, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 4
        val result = messageDao.getNearMessages(channelId, 8, limit)

        // Then
        Truth.assertThat(result.data).isEmpty()
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessages_HasNotNextIfNewestSizeEqualHalfLimit() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(1, 10, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        // should return  3, 4, 5, 6
        val loadedMessages = result.data
        val messageIds = loadedMessages.map { it.messageEntity.id }

        // Then
        Log.d("loadedMessages", "$messageIds")
        Truth.assertThat(messageIds).isEqualTo(listOf(3L, 4L, 5L, 6L))
        Truth.assertThat(loadedMessages.size).isEqualTo(limit)
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessages_HasNextIfNewestSizeBiggerHalfLimit() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
            createMessage(7, 7),
        )

        val range = listOf(
            LoadRangeEntity(1, 10, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        // should return  3, 4, 5, 6
        val loadedMessages = result.data
        val messageIds = loadedMessages.map { it.messageEntity.id }

        // Then
        Log.d("loadedMessages", "$messageIds")
        Truth.assertThat(messageIds).isEqualTo(listOf(3L, 4L, 5L, 6L))
        Truth.assertThat(loadedMessages.size).isEqualTo(limit)
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun loadNearMessages_ReturnsBalancedCenteredWindowWhenBothSidesFull() = runTest {
        // Given plenty of messages on both sides of the target inside one big range.
        val messages = (1L..20L).map { createMessage(it, it) }
        val range = listOf(LoadRangeEntity(1, 100, channelId))

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When centering on 10 with an even limit.
        val result = messageDao.getNearMessages(channelId, 10, 6)
        val messageIds = result.data.map { it.messageEntity.id }

        // Then: 3 older (incl. target) + 3 newer, target present, both flags true.
        Truth.assertThat(messageIds).isEqualTo(listOf(8L, 9L, 10L, 11L, 12L, 13L))
        Truth.assertThat(messageIds).contains(10L)
        Truth.assertThat(result.hasPrev).isTrue()
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun loadNearMessages_BorrowsFromNewestWhenOlderSideShort() = runTest {
        // Given the target has only one older message but many newer (borrow branch).
        val messages = (1L..20L).map { createMessage(it, it) }
        val range = listOf(LoadRangeEntity(1, 100, channelId))

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When centering on 2 with limit 6: only 1,2 are <= target.
        val result = messageDao.getNearMessages(channelId, 2, 6)
        val messageIds = result.data.map { it.messageEntity.id }

        // Then: window still fills to the limit by taking extra from the newer side.
        Truth.assertThat(messageIds).isEqualTo(listOf(1L, 2L, 3L, 4L, 5L, 6L))
        Truth.assertThat(result.data.size).isEqualTo(6)
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun loadNearMessages_ExcludesPendingMessagesInsideRange() = runTest {
        // Given a pending message (id 3) sits inside the range between listed messages.
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3, MessageDeliveryStatus.Pending),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )
        val range = listOf(LoadRangeEntity(1, 6, channelId))

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val result = messageDao.getNearMessages(channelId, 4, 10)
        val messageIds = result.data.map { it.messageEntity.id }

        // Then: pending id 3 is excluded from the near window.
        Truth.assertThat(messageIds).doesNotContain(3L)
        Truth.assertThat(messageIds).isEqualTo(listOf(1L, 2L, 4L, 5L, 6L))
    }

    @Test
    fun loadNearMessages_HasPrevAndNextWhenOutOfRangeMessagesExistInDb() = runTest {
        // Given DB holds messages older (1,2) and newer (10) than the load range [3-6].
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(5, 5),
            createMessage(6, 6),
            createMessage(10, 10),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 4
        val result = messageDao.getNearMessages(channelId, 5, limit)
        val messageIds = result.data.map { it.messageEntity.id }

        // Then: only in-range messages returned, but both flags true so the loader stays
        // and a server fetch can bridge the gap to the out-of-range messages.
        Truth.assertThat(messageIds).isEqualTo(listOf(3L, 5L, 6L))
        Truth.assertThat(result.hasPrev).isTrue()
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun loadNearMessages_HasPrevOnlyWhenOlderOutOfRangeMessagesExistInDb() = runTest {
        // Given older messages (1,2) exist out of range, nothing newer than the range [3-6].
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val result = messageDao.getNearMessages(channelId, 5, 4)

        // Then
        Truth.assertThat(result.data.map { it.messageEntity.id }).isEqualTo(listOf(3L, 5L, 6L))
        Truth.assertThat(result.hasPrev).isTrue()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessages_HasNextOnlyWhenNewerOutOfRangeMessagesExistInDb() = runTest {
        // Given a newer message (10) exists out of range, nothing older than the range [3-6].
        val messages = listOf(
            createMessage(3, 3),
            createMessage(5, 5),
            createMessage(6, 6),
            createMessage(10, 10),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val result = messageDao.getNearMessages(channelId, 5, 4)

        // Then
        Truth.assertThat(result.data.map { it.messageEntity.id }).isEqualTo(listOf(3L, 5L, 6L))
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isTrue()
    }

    @Test
    fun loadNearMessages_NoPrevNorNextWhenRangeCoversAllDbMessages() = runTest {
        // Given the load range covers every message in DB (no out-of-range segments).
        val messages = listOf(
            createMessage(3, 3),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val result = messageDao.getNearMessages(channelId, 5, 4)

        // Then: all in-range messages returned and nothing exists beyond the range.
        Truth.assertThat(result.data.map { it.messageEntity.id }).isEqualTo(listOf(3L, 5L, 6L))
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessages_PendingAndUnlistedOutOfRangeMessagesDoNotSetFlags() = runTest {
        // Given the only out-of-range messages are pending / unlisted -> must be ignored.
        val messages = listOf(
            createMessage(1, 1, MessageDeliveryStatus.Pending),
            createMessage(3, 3),
            createMessage(5, 5),
            createMessage(6, 6),
            createMessage(10, 10).copy(unList = true),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val result = messageDao.getNearMessages(channelId, 5, 4)

        // Then
        Truth.assertThat(result.data.map { it.messageEntity.id }).isEqualTo(listOf(3L, 5L, 6L))
        Truth.assertThat(result.hasPrev).isFalse()
        Truth.assertThat(result.hasNext).isFalse()
    }

    @Test
    fun loadNearMessages_HasNotNextIfNewestSizeNotBiggerHalfLimit() = runTest {
        // Given
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
        )

        val range = listOf(
            LoadRangeEntity(1, 10, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        // When
        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        val loadedMessages = result.data
        val messageIds = loadedMessages.map { it.messageEntity.id }

        // Then
        Log.d("loadedMessages", "$messageIds")
        Truth.assertThat(messageIds).isEqualTo(listOf(2L, 3L, 4L, 5L))
        Truth.assertThat(loadedMessages.size).isEqualTo(limit)
        Truth.assertThat(result.hasNext).isFalse()
    }
}