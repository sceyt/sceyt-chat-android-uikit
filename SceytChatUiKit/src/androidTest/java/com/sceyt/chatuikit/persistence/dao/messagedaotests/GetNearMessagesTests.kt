package com.sceyt.chatuikit.persistence.dao.messagedaotests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chat.models.message.DeliveryStatus
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

    private fun createMessage(tid: Long, id: Long, deliveryStatus: DeliveryStatus = DeliveryStatus.Displayed): MessageEntity {
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
            disableMentionsCount = false
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