package com.sceyt.sceytchatuikit.persistence.dao.messagedaotests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.persistence.dao.LoadRangeDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.LoadRangeEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
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
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
        rangeDao = database.loadRangeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createMessage(tid: Long, id: Long): MessageEntity {
        return MessageEntity(tid, id, channelId, "body", "text", null, id, 0, incoming = false, isTransient = false, silent = false,
            deliveryStatus = DeliveryStatus.Displayed, state = MessageState.Unmodified, fromId = "1", markerCount = null, mentionedUsersIds = null, parentId = null, replyCount = 0L, displayCount = 0, autoDeleteAt = null, forwardingDetailsDb = null, bodyAttribute = null, unList = false)
    }

    @Test
    fun loadNearMessagesShouldReturnMessagesInRange() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 6
        val result = messageDao.getNearMessages(channelId, 3, limit)
        // should return 1, 2, 3, 4
        val loadedMessages = result.data

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")

        Truth.assertThat(
            loadedMessages.size == 4
                    && loadedMessages.maxBy { it.messageEntity.id ?: 0L }.messageEntity.id == 4L
                    && loadedMessages.minBy { it.messageEntity.id ?: 0L }.messageEntity.id == 1L
        ).isTrue()
    }

    @Test
    fun loadNearMessagesShouldReturnMessagesInRangeCase2() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 10
        val result = messageDao.getNearMessages(channelId, 5, limit)
        // should return 3, 4, 5
        val loadedMessages = result.data

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")
        Truth.assertThat(
            loadedMessages.size == 3
                    && loadedMessages.map { it.messageEntity.id }.sortedBy { it } == listOf(3L, 4L, 5L)
        ).isTrue()
    }

    @Test
    fun loadNearMessagesShouldReturnEmptyIfRangeNotFoundForCurrentMessage_HasNextFalseHasPrevFalse() = runTest {
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        messageDao.upsertMessageEntities(messages)

        val loadedMessages = messageDao.getNearMessages(channelId, 6, 10)

        Truth.assertThat(
            loadedMessages.data.isEmpty() && !loadedMessages.hasPrev && !loadedMessages.hasNext
        ).isTrue()
    }

    @Test
    fun loadNearMessagesShouldNotReturnMessagesOutOfRange_HasNextFalseHasPrevFalse() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val loadedMessages = messageDao.getNearMessages(channelId, 8, limit)
        Truth.assertThat(
            loadedMessages.data.isEmpty() && !loadedMessages.hasPrev && !loadedMessages.hasNext
        ).isTrue()
    }


    @Test
    fun loadNearMessages_HasNextIfNewestSizeEqualHalfLimit() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        // should return  3, 4, 5, 6
        val loadedMessages = result.data

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")

        Truth.assertThat(
            loadedMessages.size == limit
                    && loadedMessages.maxBy { it.messageEntity.id ?: 0L }.messageEntity.id == 6L
                    && loadedMessages.minBy { it.messageEntity.id ?: 0L }.messageEntity.id == 3L
                    && result.hasNext
        ).isTrue()
    }

    @Test
    fun loadNearMessages_HasNextIfNewestSizeBiggerHalfLimit() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        // should return  3, 4, 5, 6
        val loadedMessages = result.data

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")

        Truth.assertThat(
            loadedMessages.size == limit
                    && loadedMessages.maxBy { it.messageEntity.id ?: 0L }.messageEntity.id == 6L
                    && loadedMessages.minBy { it.messageEntity.id ?: 0L }.messageEntity.id == 3L
                    && result.hasNext
        ).isTrue()
    }

    @Test
    fun loadNearMessages_HasNotNextIfNewestSizeNotBiggerHalfLimit() = runTest {
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

        messageDao.upsertMessageEntities(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val result = messageDao.getNearMessages(channelId, 4, limit)
        // should return  2, 3, 4, 5
        val loadedMessages = result.data

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")

        Truth.assertThat(
            loadedMessages.size == limit
                    && loadedMessages.maxBy { it.messageEntity.id ?: 0L }.messageEntity.id == 5L
                    && loadedMessages.minBy { it.messageEntity.id ?: 0L }.messageEntity.id == 2L
                    && !result.hasNext
        ).isTrue()
    }
}