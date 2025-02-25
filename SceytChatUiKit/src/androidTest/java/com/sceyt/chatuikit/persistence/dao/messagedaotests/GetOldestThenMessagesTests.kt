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
class GetOldestThenMessagesTests{
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
    fun loadPrevMessagesShouldReturnMessagesInRange() = runTest {
        val messages = listOf(
             createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(1, 5, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val loadedMessages = messageDao.getOldestThenMessages(channelId, 5, limit)

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")

        Truth.assertThat(
            loadedMessages.size == limit
                    && loadedMessages.maxBy { it.messageEntity.id ?: 0L }.messageEntity.id == 4L
                    && loadedMessages.minBy { it.messageEntity.id ?: 0L }.messageEntity.id == 1L
        ).isTrue()
    }

    @Test
    fun loadPrevMessagesShouldReturnMessagesInRangeCase2() = runTest {
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(3, 6, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        val loadedMessages = messageDao.getOldestThenMessages(channelId, 6, 10)
        // should return 3, 4, 5

        Log.d("loadedMessages", "${loadedMessages.map { it.messageEntity.id }}")
        Truth.assertThat(
            loadedMessages.size == 3
                    && loadedMessages.map { it.messageEntity.id }.sortedBy { it } == listOf(3L, 4L, 5L)
        ).isTrue()
    }

    @Test
    fun loadPrevMessagesShouldReturnEmptyIfRangeNotFoundForCurrentMessage() = runTest {
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)

        val loadedMessages = messageDao.getOldestThenMessages(channelId, 6, 10)

        Truth.assertThat(
            loadedMessages.isEmpty()
        ).isTrue()
    }

    @Test
    fun loadPrevMessagesShouldNotReturnMessagesOutOfRange() = runTest {
        val messages = listOf(
            createMessage(1, 1),
            createMessage(2, 2),
            createMessage(3, 3),
            createMessage(4, 4),
            createMessage(5, 5),
            createMessage(6, 6),
        )

        val range = listOf(
            LoadRangeEntity(5, 8, channelId)
        )

        messageDao.upsertMessageEntitiesWithTransaction(messages)
        rangeDao.insertAll(range)

        val limit = 4
        val loadedMessages = messageDao.getOldestThenMessages(channelId, 5, limit)
        Truth.assertThat(
            loadedMessages.isEmpty()
        ).isTrue()
    }
}