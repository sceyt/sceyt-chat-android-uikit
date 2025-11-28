package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CheckDeletedMessagesByRangeUseCaseTest {
    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var messagesCache: MessagesCache
    private lateinit var useCase: CheckDeletedMessagesByRangeUseCase

    private val channelId = 123L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()

        messageDao = database.messageDao()
        messagesCache = MessagesCache()
        useCase = CheckDeletedMessagesByRangeUseCase(messageDao, messagesCache)
    }

    @After
    fun tearDown() {
        database.close()
    }


    // ========== Case 1: Empty Response Tests ==========

    // ========== LoadNear Specific Tests ==========

    @Test
    fun loadNear_shouldDeleteMessagesWithinReturnedRangeOnly() = runTest {
        // Arrange - LoadNear returns messages around messageId
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Before range - should remain
            createMessageEntity(tid = 90, id = 90),   // In range - returned
            createMessageEntity(tid = 95, id = 95),   // In range - DELETED on server
            createMessageEntity(tid = 100, id = 100), // In range - returned (messageId)
            createMessageEntity(tid = 105, id = 105), // In range - DELETED on server
            createMessageEntity(tid = 110, id = 110), // In range - returned
            createMessageEntity(tid = 200, id = 200)  // After range - should remain
        )

        val serverMessages = listOf(
            createSceytMessage(id = 90),
            createSceytMessage(id = 100),
            createSceytMessage(id = 110)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Should only delete within range [90, 110], keep 50 and 200
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(50L, 90L, 100L, 110L, 200L)
        assertThat(remainingIds).doesNotContain(95L)  // Deleted (in range)
        assertThat(remainingIds).doesNotContain(105L) // Deleted (in range)
        assertThat(remainingIds).contains(50L)  // Kept (before range)
        assertThat(remainingIds).contains(200L) // Kept (after range)
    }

    @Test
    fun loadNear_shouldNotDeleteWhenAllMessagesMatch() = runTest {
        // Arrange
        insertMessages(
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 90),
            createSceytMessage(id = 100),
            createSceytMessage(id = 110)
        )

        val initialCount = messageDao.getMessagesCount(channelId)

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        assertThat(finalCount).isEqualTo(initialCount)
    }

    @Test
    fun loadNear_withSingleMessage_shouldNotDeleteAnything() = runTest {
        // Arrange
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 150, id = 150)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100) // Single message
        )

        val initialCount = messageDao.getMessagesCount(channelId)

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Single message, no range to check
        val finalCount = messageDao.getMessagesCount(channelId)
        assertThat(finalCount).isEqualTo(initialCount)
    }

    @Test
    fun emptyResponse_withLoadNear_shouldNotDeleteAnythingIfReceivedEmptyList() = runTest {
        // Arrange - Insert some messages
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 200, id = 200),
            createMessageEntity(tid = 300, id = 300)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 1000,
            limit = 30,
            serverMessages = emptyList()
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(initialCount)
        assertThat(finalCount).isEqualTo(3)
    }

    @Test
    fun emptyResponse_withLoadNear_shouldNotDeleteAnythingIfReceivedSingleItem() = runTest {
        // Arrange - Insert some messages
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 200, id = 200),
            createMessageEntity(tid = 300, id = 300)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 1000,
            limit = 30,
            serverMessages = listOf(createSceytMessage(id = 200))
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(initialCount)
        assertThat(finalCount).isEqualTo(3)
    }

    @Test
    fun emptyResponse_withLoadPrev_shouldDeleteMessagesLowerThanOrEqualToLastMessageId() = runTest {
        // Arrange - Insert messages with IDs around lastMessageId
        insertMessages(
            createMessageEntity(tid = 800, id = 800),  // Should be deleted
            createMessageEntity(tid = 900, id = 900),  // Should be deleted
            createMessageEntity(tid = 1000, id = 1000), // Should be deleted (lastMessageId)
            createMessageEntity(tid = 1100, id = 1100), // Should remain
            createMessageEntity(tid = 1200, id = 1200)  // Should remain
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = emptyList()
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(1100L, 1200L)
    }

    @Test
    fun emptyResponse_withLoadNext_shouldDeleteMessagesGreaterThanOrEqualToLastMessageId() =
        runTest {
            // Arrange
            insertMessages(
                createMessageEntity(tid = 800, id = 800),   // Should remain
                createMessageEntity(tid = 900, id = 900),   // Should remain
                createMessageEntity(tid = 1000, id = 1000), // Should be deleted (lastMessageId)
                createMessageEntity(tid = 1100, id = 1100), // Should be deleted
                createMessageEntity(tid = 1200, id = 1200)  // Should be deleted
            )

            Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

            // Act
            useCase(
                channelId = channelId,
                loadType = LoadType.LoadNext,
                messageId = 1000,
                limit = 30,
                serverMessages = emptyList()
            )

            // Assert
            val remainingIds = messageDao.getMessagesIds(channelId)
            Log.d("Test", "Remaining messages: $remainingIds")
            assertThat(remainingIds).containsExactly(800L, 900L)
        }

    @Test
    fun emptyResponse_withLoadPrev_shouldNotDeletePendingMessages() = runTest {
        // Arrange - Include a pending message
        insertMessages(
            createMessageEntity(tid = 800, id = 800, deliveryStatus = DeliveryStatus.Sent),
            createMessageEntity(
                tid = 900,
                id = 900,
                deliveryStatus = DeliveryStatus.Pending
            ), // Pending - should remain
            createMessageEntity(tid = 1000, id = 1000, deliveryStatus = DeliveryStatus.Sent)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = emptyList()
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).contains(900L) // Pending message should remain
    }

    // ========== Case 2: Single Message Edge Case Tests ==========

    @Test
    fun singleMessageEdgeCase_withLoadPrev_shouldDeleteAllMessagesBeforeMessageId() = runTest {
        // Arrange - Only messageId itself is returned (reached end at that message)
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should be deleted (< messageId)
            createMessageEntity(tid = 80, id = 80),   // Should be deleted (< messageId)
            createMessageEntity(tid = 100, id = 100), // The messageId itself - should remain
            createMessageEntity(tid = 200, id = 200)  // Should remain (> messageId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100) // Only messageId returned
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Should delete < messageId only (not including messageId itself)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L)
        assertThat(remainingIds).doesNotContain(50L)
        assertThat(remainingIds).doesNotContain(80L)
    }

    @Test
    fun singleMessageEdgeCase_withLoadNext_shouldDeleteAllMessagesAfterMessageId() = runTest {
        // Arrange - Only messageId itself is returned (reached end at that message)
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should remain (< messageId)
            createMessageEntity(tid = 100, id = 100), // The messageId itself - should remain
            createMessageEntity(tid = 150, id = 150), // Should be deleted (> messageId)
            createMessageEntity(tid = 200, id = 200)  // Should be deleted (> messageId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100) // Only messageId returned
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Should delete > messageId only (not including messageId itself)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(50L, 100L)
        assertThat(remainingIds).doesNotContain(150L)
        assertThat(remainingIds).doesNotContain(200L)
    }

    @Test
    fun singleMessageEdgeCase_withLoadNewest_shouldDeleteAllMessagesAfterMessageId() = runTest {
        // Arrange - LoadNewest loads from the newest messages
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should remain
            createMessageEntity(tid = 100, id = 100), // The messageId itself - should remain  
            createMessageEntity(tid = 150, id = 150), // Should be deleted (newer than messageId)
            createMessageEntity(tid = 200, id = 200)  // Should be deleted (newer than messageId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100) // Only messageId returned
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNewest,
            messageId = Long.MAX_VALUE, // LoadNewest typically uses MAX_VALUE
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Should keep messages <= 100, delete > 100
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).contains(50L)
        assertThat(remainingIds).contains(100L)
        assertThat(remainingIds).doesNotContain(150L)
        assertThat(remainingIds).doesNotContain(200L)
    }

    // ========== Case 3: Extended Range Tests (Gap Detection via Range Adjustment) ==========

    @Test
    fun extendedRange_withLoadPrev_shouldDeleteMessagesInGapBetweenReturnedRangeAndMessageId() =
        runTest {
            // Arrange - Request from messageId=1000, server returns 30-50
            // Range extended to [30, 999] to include gap, messages 51-999 should be deleted
            insertMessages(
                createMessageEntity(tid = 30, id = 30),
                createMessageEntity(tid = 40, id = 40),
                createMessageEntity(tid = 50, id = 50),
                createMessageEntity(tid = 60, id = 60),  // In gap - should be deleted
                createMessageEntity(tid = 70, id = 70),  // In gap - should be deleted
                createMessageEntity(tid = 80, id = 80),  // In gap - should be deleted
                createMessageEntity(tid = 999, id = 999) // In gap - should be deleted
            )

            val serverMessages = listOf(
                createSceytMessage(id = 30),
                createSceytMessage(id = 40),
                createSceytMessage(id = 50)
            )

            Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

            // Act
            useCase(
                channelId = channelId,
                loadType = LoadType.LoadPrev,
                messageId = 1000,
                limit = 30,
                serverMessages = serverMessages
            )

            // Assert
            val remainingIds = messageDao.getMessagesIds(channelId)
            Log.d("Test", "Remaining messages: $remainingIds")
            assertThat(remainingIds).containsExactly(30L, 40L, 50L)
            assertThat(remainingIds).doesNotContain(60L)
            assertThat(remainingIds).doesNotContain(70L)
            assertThat(remainingIds).doesNotContain(80L)
            assertThat(remainingIds).doesNotContain(999L)
        }

    @Test
    fun extendedRange_withLoadNext_shouldDeleteMessagesInGapBetweenMessageIdAndReturnedRange() =
        runTest {
            // Arrange - Request from messageId=100, server returns 150-180
            // Range extended to [101, 170] to include gap, messages 101-149 should be deleted
            insertMessages(
                createMessageEntity(tid = 101, id = 101), // In gap - should be deleted
                createMessageEntity(tid = 120, id = 120), // In gap - should be deleted
                createMessageEntity(tid = 149, id = 149), // In gap - should be deleted
                createMessageEntity(tid = 150, id = 150),
                createMessageEntity(tid = 160, id = 160),
                createMessageEntity(tid = 170, id = 170)
            )

            val serverMessages = listOf(
                createSceytMessage(id = 150),
                createSceytMessage(id = 160),
                createSceytMessage(id = 170)
            )

            Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

            // Act
            useCase(
                channelId = channelId,
                loadType = LoadType.LoadNext,
                messageId = 100,
                limit = 30,
                serverMessages = serverMessages
            )

            // Assert
            val remainingIds = messageDao.getMessagesIds(channelId)
            Log.d("Test", "Remaining messages: $remainingIds")
            assertThat(remainingIds).containsExactly(150L, 160L, 170L)
            assertThat(remainingIds).doesNotContain(101L)
            assertThat(remainingIds).doesNotContain(120L)
            assertThat(remainingIds).doesNotContain(149L)
        }

    @Test
    fun extendedRange_withLoadPrev_shouldNotDeleteWhenNoGap() = runTest {
        // Arrange - Request from messageId=100, server returns 90-99 (no gap)
        // Range stays [90, 99], no extension needed
        insertMessages(
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 95, id = 95),
            createMessageEntity(tid = 99, id = 99)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 90),
            createSceytMessage(id = 95),
            createSceytMessage(id = 99)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(initialCount)
        assertThat(finalCount).isEqualTo(3)
    }

    @Test
    fun extendedRange_withLoadNext_shouldNotDeleteWhenNoGap() = runTest {
        // Arrange - Request from messageId=100, server returns 101-110 (no gap)
        // Range stays [101, 110], no extension needed
        insertMessages(
            createMessageEntity(tid = 101, id = 101),
            createMessageEntity(tid = 105, id = 105),
            createMessageEntity(tid = 110, id = 110)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 101),
            createSceytMessage(id = 105),
            createSceytMessage(id = 110)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(initialCount)
        assertThat(finalCount).isEqualTo(3)
    }

    // ========== Case 4: Reached End of Messages Tests ==========

    @Test
    fun reachedEnd_withLoadPrev_shouldDeleteMessagesLessThanStartId() = runTest {
        // Arrange - Insert messages and return fewer than limit
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should be deleted (< startId)
            createMessageEntity(tid = 100, id = 100), // Should remain (startId)
            createMessageEntity(tid = 200, id = 200), // Should remain
            createMessageEntity(tid = 300, id = 300)  // Should remain (endId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act - Limit is 30, but only 3 messages returned (reached end)
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).doesNotContain(50L)
        assertThat(remainingIds).containsAtLeast(100L, 200L, 300L)
    }

    @Test
    fun reachedEnd_withLoadNext_shouldDeleteMessagesGreaterThanEndId() = runTest {
        // Arrange
        insertMessages(
            createMessageEntity(tid = 100, id = 100), // Should remain (startId)
            createMessageEntity(tid = 200, id = 200), // Should remain
            createMessageEntity(tid = 300, id = 300), // Should remain (endId)
            createMessageEntity(tid = 400, id = 400)  // Should be deleted (> endId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).doesNotContain(400L)
        assertThat(remainingIds).containsAtLeast(100L, 200L, 300L)
    }

    @Test
    fun reachedEnd_withLoadNear_shouldNotDeleteBeyondRange() = runTest {
        // Arrange - LoadNear checks only within the returned range, not beyond
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Before range - should remain
            createMessageEntity(tid = 100, id = 100), // In range
            createMessageEntity(tid = 150, id = 150), // In range - DELETED on server
            createMessageEntity(tid = 200, id = 200), // In range
            createMessageEntity(tid = 300, id = 300), // In range
            createMessageEntity(tid = 400, id = 400)  // After range - should remain
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 200,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert - Should delete 150 (within range), keep 50 and 400 (beyond range)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(50L, 100L, 200L, 300L, 400L)
        assertThat(remainingIds).doesNotContain(150L) // Deleted within range
        assertThat(remainingIds).contains(50L)  // Kept (before range)
        assertThat(remainingIds).contains(400L) // Kept (after range)
    }

    // ========== Case 5: Messages Within Range Tests ==========

    @Test
    fun withinRange_shouldDeleteMessagesThatExistLocallyButNotInServerResponse() = runTest {
        // Arrange - Local DB has messages 100, 150, 200, 250, 300
        // Server returns only 100, 200, 300 (150 and 250 were deleted)
        insertMessages(
            createMessageEntity(tid = 1100, id = 100),
            createMessageEntity(tid = 1150, id = 150), // Deleted on server
            createMessageEntity(tid = 1200, id = 200),
            createMessageEntity(tid = 1250, id = 250), // Deleted on server
            createMessageEntity(tid = 1300, id = 300)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100, tid = 1100),
            createSceytMessage(id = 200, tid = 1200),
            createSceytMessage(id = 300, tid = 1300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L, 300L)
        assertThat(remainingIds).doesNotContain(150L)
        assertThat(remainingIds).doesNotContain(250L)
    }

    @Test
    fun withinRange_shouldNotDeleteWhenAllLocalMessagesMatchServerMessages() = runTest {
        // Arrange - All local messages exist in server response
        insertMessages(
            createMessageEntity(tid = 1100, id = 100),
            createMessageEntity(tid = 1200, id = 200),
            createMessageEntity(tid = 1300, id = 300)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100, tid = 1100),
            createSceytMessage(id = 200, tid = 1200),
            createSceytMessage(id = 300, tid = 1300)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(initialCount)
        assertThat(finalCount).isEqualTo(3)
    }

    @Test
    fun withinRange_shouldHandleSingleMessageCorrectly() = runTest {
        // Arrange
        insertMessages(
            createMessageEntity(tid = 1100, id = 100)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100, tid = 1100)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L)
    }

    @Test
    fun withinRange_shouldHandleMultipleDeletedMessagesCorrectly() = runTest {
        // Arrange - Local has 100-500, server returns only 100 and 500
        insertMessages(
            createMessageEntity(tid = 1100, id = 100),
            createMessageEntity(tid = 1200, id = 200), // Deleted
            createMessageEntity(tid = 1300, id = 300), // Deleted
            createMessageEntity(tid = 1400, id = 400), // Deleted
            createMessageEntity(tid = 1500, id = 500)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100, tid = 1100),
            createSceytMessage(id = 500, tid = 1500)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 500L)
    }

    @Test
    fun withinRange_shouldHandleUnsortedServerMessagesCorrectly() = runTest {
        // Arrange - Server returns unsorted messages
        insertMessages(
            createMessageEntity(tid = 1100, id = 100),
            createMessageEntity(tid = 1150, id = 150), // Deleted
            createMessageEntity(tid = 1200, id = 200),
            createMessageEntity(tid = 1300, id = 300)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 300, tid = 1300), // Unsorted
            createSceytMessage(id = 100, tid = 1100),
            createSceytMessage(id = 200, tid = 1200)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L, 300L)
        assertThat(remainingIds).doesNotContain(150L)
    }

    @Test
    fun limitEqualsSize_withLoadPrev_shouldStillDetectGapDeletions() = runTest {
        // Arrange - LoadPrev from 1000 returns [100, 200, 300]
        // This means messages 301-999 don't exist (server would return closest 3)
        // So message 400 should be deleted even though limit == size
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should remain (< range)
            createMessageEntity(tid = 100, id = 100), // Returned by server
            createMessageEntity(tid = 200, id = 200), // Returned by server
            createMessageEntity(tid = 300, id = 300), // Returned by server
            createMessageEntity(
                tid = 400,
                id = 400
            )  // Should be DELETED (gap between returned and messageId)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act - limit equals serverMessages.size
        // Since server returned [100,200,300], any message between 300 and 1000 was deleted
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 3,
            serverMessages = serverMessages
        )

        // Assert - 400 should be deleted (in gap), 50 should remain (before range)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(50L, 100L, 200L, 300L)
        assertThat(remainingIds).doesNotContain(400L) // Deleted because it's in the gap
    }

    // ========== Combined Scenarios ==========

    @Test
    fun shouldHandleBothRangeCheckAndEndReached() = runTest {
        // Arrange - Test both deletions within range and beyond range
        insertMessages(
            createMessageEntity(tid = 50, id = 50),   // Should be deleted (< startId)
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 150, id = 150), // Deleted on server
            createMessageEntity(tid = 200, id = 200),
            createMessageEntity(tid = 300, id = 300)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act - Reached end (limit > size) AND message deleted in range
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = serverMessages
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L, 300L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted because < startId
        assertThat(remainingIds).doesNotContain(150L) // Deleted because not in server response
    }


    // ========== Helper Methods ==========

    private fun createMessageEntity(
        tid: Long,
        id: Long,
        channelId: Long = this.channelId,
        deliveryStatus: DeliveryStatus = DeliveryStatus.Sent
    ): MessageEntity {
        return MessageEntity(
            tid = tid,
            id = id,
            channelId = channelId,
            body = "Test message $id",
            type = "text",
            bodyAttribute = null,
            createdAt = id,
            incoming = false,
            isTransient = false,
            silent = false,
            deliveryStatus = deliveryStatus,
            state = MessageState.Unmodified,
            fromId = "user_1",
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
    }

    private fun createSceytMessage(
        id: Long,
        tid: Long = id * 10,
        channelId: Long = this.channelId
    ): SceytMessage {
        return SceytMessage(
            id = id,
            tid = tid,
            channelId = channelId,
            body = "Test message $id",
            type = "text",
            metadata = null,
            createdAt = id,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            deliveryStatus = DeliveryStatus.Sent,
            state = MessageState.Unmodified,
            user = null,
            attachments = null,
            userReactions = null,
            reactionTotals = null,
            markerTotals = null,
            userMarkers = null,
            mentionedUsers = null,
            parentMessage = null,
            replyCount = 0,
            displayCount = 0,
            autoDeleteAt = null,
            forwardingDetails = null,
            pendingReactions = null,
            bodyAttributes = null,
            disableMentionsCount = false,
            poll = null
        )
    }

    private suspend fun insertMessages(vararg messages: MessageEntity) {
        messageDao.upsertMessageEntitiesWithTransaction(messages.toList())
    }
}

