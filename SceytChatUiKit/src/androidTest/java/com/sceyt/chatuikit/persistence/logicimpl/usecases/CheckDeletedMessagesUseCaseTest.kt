package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
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
class CheckDeletedMessagesUseCaseTest {
    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var messagesCache: MessagesCache
    private lateinit var deleteByLoadType: HandleDeleteMessagesByLoadTypeUseCase
    private lateinit var handleMessagesInRange: HandleMessagesInRangeUseCase
    private lateinit var deletedNearMessagesUseCase: CheckDeletedNearMessagesUseCase
    private lateinit var useCase: CheckDeletedMessagesUseCase

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
        deleteByLoadType = HandleDeleteMessagesByLoadTypeUseCase(messageDao, messagesCache)
        handleMessagesInRange = HandleMessagesInRangeUseCase(messageDao, messagesCache)
        deletedNearMessagesUseCase = CheckDeletedNearMessagesUseCase(
            messageDao, messagesCache, deleteByLoadType, handleMessagesInRange
        )
        useCase = CheckDeletedMessagesUseCase(
            deletedNearMessagesUseCase, deleteByLoadType, handleMessagesInRange
        )
    }

    @After
    fun tearDown() {
        database.close()
    }


    // ========== Case 1: Empty Response Tests ==========

    // ========== LoadNear Specific Tests ==========

    @Test
    fun loadNear_shouldDeleteMessagesWithinReturnedRangeOnly() = runTest {
        // Arrange - LoadNear returns messages around messageId with limit == size
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

        // Act - limit == size, so only within-range deletion (50 and 200 stay)
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 100,
            limit = 3, // Same as serverMessages.size to avoid "reached end" logic
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
        )

        // Assert
        val finalCount = messageDao.getMessagesCount(channelId)
        assertThat(finalCount).isEqualTo(initialCount)
    }

    @Test
    fun loadNear_withSingleMessage_shouldDeleteAllOtherMessages() = runTest {
        // Arrange
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 150, id = 150)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100) // Single message
        )

        // Act - size (1) < limit (30), so delete all except returned message
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages,
            syncStartTime = 0L
        )

        // Assert - Only the returned message should remain (50 and 150 deleted)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(100L)
        assertThat(remainingIds).doesNotContain(50L)
        assertThat(remainingIds).doesNotContain(150L)
    }

    @Test
    fun emptyResponse_withLoadNear_shouldDeleteAllMessagesExceptPending() = runTest {
        // Arrange - Insert some messages
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 200, id = 200),
            createMessageEntity(tid = 300, id = 300)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act - Empty response means server has no messages, delete all
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 1000,
            limit = 30,
            serverMessages = emptyList(),
            syncStartTime = 0L
        )

        // Assert - All messages deleted (except pending, but none are pending here)
        val finalCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Final message count: $finalCount")
        assertThat(finalCount).isEqualTo(0)
    }

    @Test
    fun singleItem_withLoadNear_shouldDeleteAllExceptReturnedMessage() = runTest {
        // Arrange - Insert some messages
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 200, id = 200),
            createMessageEntity(tid = 300, id = 300)
        )

        val initialCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Initial message count: $initialCount")

        // Act - size (1) < limit (30), delete all except returned message
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 1000,
            limit = 30,
            serverMessages = listOf(createSceytMessage(id = 200)),
            syncStartTime = 0
        )

        // Assert - Only message 200 remains
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(200L)
        assertThat(remainingIds).doesNotContain(100L)
        assertThat(remainingIds).doesNotContain(300L)
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
            serverMessages = emptyList(),
            syncStartTime = 0L
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
                serverMessages = emptyList(),
                syncStartTime = 0
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
            createMessageEntity(tid = 800, id = 800, deliveryStatus = MessageDeliveryStatus.Sent),
            createMessageEntity(
                tid = 900,
                id = 900,
                deliveryStatus = MessageDeliveryStatus.Pending
            ), // Pending - should remain
            createMessageEntity(tid = 1000, id = 1000, deliveryStatus = MessageDeliveryStatus.Sent)
        )

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 1000,
            limit = 30,
            serverMessages = emptyList(),
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
                serverMessages = serverMessages,
                syncStartTime = 0
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
                serverMessages = serverMessages,
                syncStartTime = 0
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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

        // Act - limit == size to avoid "reached end" behavior
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNear,
            messageId = 200,
            limit = 3, // Same as serverMessages.size
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
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
            serverMessages = serverMessages,
            syncStartTime = 0L
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L, 300L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted because < startId
        assertThat(remainingIds).doesNotContain(150L) // Deleted because not in server response
    }


    // ========== syncStartTime Tests ==========

    @Test
    fun loadNext_withSyncStartTime_shouldNotDeleteMessagesCreatedAfterSyncStartTime() = runTest {
        // Arrange - Create messages with various timestamps
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // Base message - will be deleted (includeMessage=true)
            createMessageEntity(tid = 200, id = 200, createdAt = 900),   // Old message - should be deleted
            createMessageEntity(tid = 300, id = 300, createdAt = 1100),  // New message - should NOT be deleted
            createMessageEntity(tid = 400, id = 400, createdAt = 1200)   // New message - should NOT be deleted
        )

        // Server returns empty (messages 100+ were "deleted" on server)
        // But messages 300 and 400 were created AFTER sync started, so they're new messages
        val serverMessages = emptyList<SceytMessage>()

        Log.d("Test", "Initial messages: ${messageDao.getMessagesIds(channelId)}")

        // Act - Empty response means delete all >= messageId (includeMessage=true), but respect syncStartTime
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Should delete 100 and 200 (old), but keep 300 and 400 (new)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages after sync: $remainingIds")
        assertThat(remainingIds).containsExactly(300L, 400L)
        assertThat(remainingIds).doesNotContain(100L) // Deleted (includeMessage=true)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (created before syncStartTime)
        assertThat(remainingIds).contains(300L) // Kept (created after syncStartTime)
        assertThat(remainingIds).contains(400L) // Kept (created after syncStartTime)
    }

    @Test
    fun loadNext_withSyncStartTime_shouldDeleteMessagesExactlyAtBoundary() = runTest {
        // Arrange - Test boundary condition: message created AT syncStartTime
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // Base - will be deleted (includeMessage=true)
            createMessageEntity(tid = 200, id = 200, createdAt = 999),   // Before boundary - should delete
            createMessageEntity(tid = 300, id = 300, createdAt = 1000),  // AT boundary - should KEEP (>= not >)
            createMessageEntity(tid = 400, id = 400, createdAt = 1001)   // After boundary - should KEEP
        )

        val serverMessages = emptyList<SceytMessage>()

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Messages with createdAt < syncStartTime deleted, >= syncStartTime kept
        // Condition is `createdAt < syncStartTime`, so message at exactly syncStartTime is KEPT
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(300L, 400L)
        assertThat(remainingIds).doesNotContain(100L) // Deleted (includeMessage=true in empty response)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (createdAt=999 < syncStartTime=1000)
        assertThat(remainingIds).contains(300L)       // Kept (createdAt=1000 >= syncStartTime=1000)
        assertThat(remainingIds).contains(400L)       // Kept (createdAt=1001 > syncStartTime=1000)
    }

    @Test
    fun loadNext_withSyncStartTimeZero_shouldDeleteAllMessagesInRange() = runTest {
        // Arrange - syncStartTime = 0 means delete all (backward compatibility)
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // Will be deleted (includeMessage=true)
            createMessageEntity(tid = 200, id = 200, createdAt = 900),
            createMessageEntity(tid = 300, id = 300, createdAt = 1100),
            createMessageEntity(tid = 400, id = 400, createdAt = 1200)
        )

        val serverMessages = emptyList<SceytMessage>()

        // Act - syncStartTime = 0 should delete all >= messageId
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = 0L
        )

        // Assert - All messages >= 100 should be deleted (includeMessage=true)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).isEmpty()
    }

    @Test
    fun loadNewest_withSyncStartTime_shouldNotDeleteNewMessages() = runTest {
        // Arrange - This is NOT an empty response case, server returned message 100
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // Returned by server
            createMessageEntity(tid = 200, id = 200, createdAt = 900),   // Gap, old - should delete
            createMessageEntity(tid = 300, id = 300, createdAt = 1100)   // Gap, new - should NOT delete
        )

        // Server returns message 100 (single message case, reached end)
        val serverMessages = listOf(createSceytMessage(id = 100))

        // Act - Single message case triggers "reached end" logic
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNewest,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - This goes to Case 2 (single message), then deletes messages > 100 (not including 100)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(100L, 300L)
        assertThat(remainingIds).contains(100L)       // Kept (returned by server)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (old)
        assertThat(remainingIds).contains(300L)       // Kept (new message)
    }

    @Test
    fun loadNext_reachedEnd_withSyncStartTime_shouldRespectTimeBoundary() = runTest {
        // Arrange - Simulate reached end scenario (limit > size)
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // messageId, kept as anchor
            createMessageEntity(tid = 150, id = 150, createdAt = 800),   // Gap, old - DELETE
            createMessageEntity(tid = 200, id = 200, createdAt = 200),   // Returned by server
            createMessageEntity(tid = 250, id = 250, createdAt = 900),   // Gap, old - DELETE
            createMessageEntity(tid = 300, id = 300, createdAt = 300),   // Returned by server
            createMessageEntity(tid = 350, id = 350, createdAt = 1100),  // Gap, new - KEEP
            createMessageEntity(tid = 400, id = 400, createdAt = 1200)   // Beyond range, new - KEEP
        )

        val serverMessages = listOf(
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        // Act - limit=10 > size=2, reached end. Range: [min(101,200), MAX_VALUE] = [101, MAX_VALUE]
        // Note: messageId=100 is outside the range (anchor) and stays in DB even if old
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - messageId (100) preserved as anchor, old gaps deleted, new messages kept
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining after reached end with syncStartTime: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 200L, 300L, 350L, 400L)
        assertThat(remainingIds).contains(100L)       // Kept (messageId anchor, even though old)
        assertThat(remainingIds).doesNotContain(150L) // Deleted (gap, old)
        assertThat(remainingIds).doesNotContain(250L) // Deleted (gap, old)
        assertThat(remainingIds).contains(350L)       // Kept (gap, but new)
        assertThat(remainingIds).contains(400L)       // Kept (beyond range, new)
    }

    @Test
    fun loadPrev_withSyncStartTime_shouldNotBeAffected() = runTest {
        // Arrange - LoadPrev goes backward in time, so syncStartTime shouldn't matter
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 50),     // Old message - will be deleted
            createMessageEntity(tid = 75, id = 75, createdAt = 1100),   // New message (unusual but possible) - will be deleted
            createMessageEntity(tid = 100, id = 100, createdAt = 100)   // Base message - will be deleted (includeMessage=true)
        )

        // Server returns empty for LoadPrev from 100
        val serverMessages = emptyList<SceytMessage>()

        // Act - LoadPrev should delete all <= 100, regardless of syncStartTime (includeMessage=true for empty)
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadPrev,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - All messages <= 100 should be deleted (LoadPrev doesn't use syncStartTime, includeMessage=true)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).isEmpty()
        assertThat(remainingIds).doesNotContain(50L)  // Deleted
        assertThat(remainingIds).doesNotContain(75L)  // Deleted (even though createdAt > syncStartTime)
        assertThat(remainingIds).doesNotContain(100L) // Deleted (includeMessage=true)
    }

    @Test
    fun withinRange_withSyncStartTime_shouldRespectTimeBoundary() = runTest {
        // Arrange - Within range deletion DOES use syncStartTime
        // Even gap detection should preserve new messages
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // messageId anchor - NOT in server response
            createMessageEntity(tid = 150, id = 150, createdAt = 1100),  // Gap, NEW - should KEEP
            createMessageEntity(tid = 200, id = 200, createdAt = 200),   // Returned
            createMessageEntity(tid = 250, id = 250, createdAt = 900),   // Gap, OLD - should DELETE
            createMessageEntity(tid = 300, id = 300, createdAt = 300)    // Returned
        )

        // Server excludes messageId for LoadNext, returns only messages AFTER 100
        val serverMessages = listOf(
            createSceytMessage(id = 200),
            createSceytMessage(id = 300)
        )

        // Act - limit=2 == size, so only within-range check (not reached end)
        // Range will be [101, 300] (messageId+1 to serverIds.last)
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 2,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Within range [101, 300] check respects syncStartTime
        // messageId=100 preserved (anchor, outside range)
        // 150 kept (new), 250 deleted (old)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(100L, 150L, 200L, 300L)
        assertThat(remainingIds).contains(100L)       // Kept (anchor, outside checked range)
        assertThat(remainingIds).contains(150L)       // Kept (new, created after syncStartTime)
        assertThat(remainingIds).doesNotContain(250L) // Deleted (old, created before syncStartTime)
    }

    @Test
    fun loadNext_multipleMessagesWithMixedTimestamps_shouldDeleteOnlyOldOnes() = runTest {
        // Arrange - Complex scenario with multiple messages at various timestamps
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100), // Old, should delete because server returns empty
            createMessageEntity(tid = 150, id = 150, createdAt = 500),   // Old, should delete
            createMessageEntity(tid = 200, id = 200, createdAt = 800),   // Old, should delete
            createMessageEntity(tid = 250, id = 250, createdAt = 1050),  // New, should keep
            createMessageEntity(tid = 300, id = 300, createdAt = 950),   // Old, should delete
            createMessageEntity(tid = 350, id = 350, createdAt = 1200),  // New, should keep
            createMessageEntity(tid = 400, id = 400, createdAt = 1300)   // New, should keep
        )

        // Server returns empty
        val serverMessages = emptyList<SceytMessage>()

        // Act
        useCase(
            channelId = channelId,
            loadType = LoadType.LoadNext,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Only old messages should be deleted
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly( 250L, 350L, 400L)
        assertThat(remainingIds).doesNotContain(150L) // Deleted (old)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (old)
        assertThat(remainingIds).doesNotContain(300L) // Deleted (old)
        assertThat(remainingIds).contains(250L)       // Kept (new)
        assertThat(remainingIds).contains(350L)       // Kept (new)
        assertThat(remainingIds).contains(400L)       // Kept (new)
    }


    // ========== Helper Methods ==========

    private fun createMessageEntity(
        tid: Long,
        id: Long,
        channelId: Long = this.channelId,
        deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Sent,
        createdAt: Long = id
    ): MessageEntity {
        return MessageEntity(
            tid = tid,
            id = id,
            channelId = channelId,
            body = "Test message $id",
            type = "text",
            bodyAttribute = null,
            createdAt = createdAt,
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
            deliveryStatus = MessageDeliveryStatus.Sent,
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

