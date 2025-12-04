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
class CheckDeletedNearMessagesUseCaseTest {
    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var messagesCache: MessagesCache
    private lateinit var deleteByLoadType: HandleDeleteMessagesByLoadTypeUseCase
    private lateinit var handleMessagesInRange: HandleMessagesInRangeUseCase
    private lateinit var useCase: CheckDeletedNearMessagesUseCase

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
        useCase = CheckDeletedNearMessagesUseCase(
            messageDao, messagesCache, deleteByLoadType, handleMessagesInRange
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== Category 1: Empty Response Tests ==========

    @Test
    fun emptyResponse_shouldDeleteAllChannelMessagesExceptPending() = runTest {
        // Arrange: Insert 10 messages (all sent)
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150),
            createMessageEntity(tid = 160, id = 160),
            createMessageEntity(tid = 170, id = 170),
            createMessageEntity(tid = 180, id = 180),
            createMessageEntity(tid = 190, id = 190)
        )

        Log.d("Test", "Initial count: ${messageDao.getMessagesCount(channelId)}")

        // Act: Call with empty server response
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 50,
            serverMessages = emptyList(),
            syncStartTime = 0L
        )

        // Assert: All sent messages deleted (no pending messages in this test)
        val remainingCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Remaining count: $remainingCount")
        assertThat(remainingCount).isEqualTo(0)
    }

    @Test
    fun emptyResponse_shouldNotDeletePendingMessages() = runTest {
        // Arrange: Insert messages including pending ones
        insertMessages(
            createMessageEntity(tid = 100, id = 100, deliveryStatus = MessageDeliveryStatus.Sent),
            createMessageEntity(tid = 110, id = 110, deliveryStatus = MessageDeliveryStatus.Pending),
            createMessageEntity(tid = 120, id = 120, deliveryStatus = MessageDeliveryStatus.Sent),
            createMessageEntity(tid = 130, id = 130, deliveryStatus = MessageDeliveryStatus.Pending)
        )

        Log.d("Test", "Initial count: ${messageDao.getMessagesCount(channelId)}")

        // Act: Call with empty response
        useCase(
            channelId = channelId,
            messageId = 110,
            limit = 50,
            serverMessages = emptyList(),
            syncStartTime =0
        )

        // Assert: Pending messages are preserved, only sent messages are deleted
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(110L, 130L) // Only pending messages remain
        assertThat(remainingIds).doesNotContain(100L) // Sent message deleted
        assertThat(remainingIds).doesNotContain(120L) // Sent message deleted
    }

    // ========== Category 2: Reached End - Size < Limit Tests ==========

    @Test
    fun reachedEnd_singleMessage_shouldDeleteAllOthers() = runTest {
        // Arrange: Insert 10 messages (IDs 100-190)
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150),
            createMessageEntity(tid = 160, id = 160),
            createMessageEntity(tid = 170, id = 170),
            createMessageEntity(tid = 180, id = 180),
            createMessageEntity(tid = 190, id = 190)
        )

        // Act: Call with messageId=150, limit=50, serverMessages=[150] (size=1 < limit)
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 50,
            serverMessages = listOf(createSceytMessage(id = 150)),
            syncStartTime = 0L
        )

        // Assert: Only message 150 remains, all others deleted
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(150L)
    }

    @Test
    fun reachedEnd_fewMessages_shouldDeleteAllNotInList() = runTest {
        // Arrange: Insert 20 messages
        val messagesToInsert = (100..290 step 10).map { id ->
            createMessageEntity(tid = id.toLong(), id = id.toLong())
        }
        insertMessages(*messagesToInsert.toTypedArray())

        val serverReturnedIds = listOf(100L, 120L, 150L, 180L, 200L)
        val serverMessages = serverReturnedIds.map { createSceytMessage(id = it) }

        // Act: Call with limit=50, return only 5 messages (size=5 < limit=50)
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 50,
            serverMessages = serverMessages,
            syncStartTime = 0L
        )

        // Assert: Only those 5 remain
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactlyElementsIn(serverReturnedIds)
    }

    @Test
    fun reachedEnd_shouldNotDeletePendingMessages() = runTest {
        // Arrange: Insert messages including pending
        insertMessages(
            createMessageEntity(tid = 100, id = 100, deliveryStatus = MessageDeliveryStatus.Sent),
            createMessageEntity(tid = 110, id = 110, deliveryStatus = MessageDeliveryStatus.Pending),
            createMessageEntity(tid = 120, id = 120, deliveryStatus = MessageDeliveryStatus.Sent),
            createMessageEntity(tid = 130, id = 130, deliveryStatus = MessageDeliveryStatus.Pending),
            createMessageEntity(tid = 140, id = 140, deliveryStatus = MessageDeliveryStatus.Sent)
        )

        // Act: Call with size < limit, return only message 120
        useCase(
            channelId = channelId,
            messageId = 120,
            limit = 50,
            serverMessages = listOf(createSceytMessage(id = 120)),
            syncStartTime = 0L
        )

        // Assert: Pending messages preserved
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).contains(110L) // Pending preserved
        assertThat(remainingIds).contains(130L) // Pending preserved
        assertThat(remainingIds).contains(120L) // Server returned
        assertThat(remainingIds).doesNotContain(100L) // Non-pending deleted
        assertThat(remainingIds).doesNotContain(140L) // Non-pending deleted
    }

    // ========== Category 3: Top/Bottom Split Logic Tests ==========

    @Test
    fun topEmpty_shouldDeletePreviousMessages() = runTest {
        // Arrange: Insert messages 50-200, messageId=100
        // Server returns only bottomNearIds (messages > 100)
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 60, id = 60),
            createMessageEntity(tid = 70, id = 70),
            createMessageEntity(tid = 80, id = 80),
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 100, id = 100), // messageId
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150)
        )

        // Act: Return only messages > 100 (bottom), topNearIds is empty
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 110),
                createSceytMessage(id = 120),
                createSceytMessage(id = 130),
                createSceytMessage(id = 140),
                createSceytMessage(id = 150)
            ),
            syncStartTime = 0L
        )

        // Assert: Messages < 110 are deleted (LoadPrev direction)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(50L, 60L, 70L, 80L, 90L, 100L)
        assertThat(remainingIds).containsAtLeast(110L, 120L, 130L, 140L, 150L)
    }

    @Test
    fun bottomEmpty_shouldDeleteNextMessages() = runTest {
        // Arrange: Insert messages 50-200, messageId=150
        // Server returns only topNearIds (messages ≤ 150)
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 60, id = 60),
            createMessageEntity(tid = 70, id = 70),
            createMessageEntity(tid = 80, id = 80),
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150), // messageId
            createMessageEntity(tid = 160, id = 160),
            createMessageEntity(tid = 170, id = 170),
            createMessageEntity(tid = 180, id = 180),
            createMessageEntity(tid = 190, id = 190),
            createMessageEntity(tid = 200, id = 200)
        )

        // Act: Return only messages ≤ 150 (top), bottomNearIds is empty
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 100),
                createSceytMessage(id = 110),
                createSceytMessage(id = 120),
                createSceytMessage(id = 130),
                createSceytMessage(id = 140),
                createSceytMessage(id = 150)
            ),
            syncStartTime = 0L
        )

        // Assert: Messages > 150 are deleted (LoadNext direction)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(160L, 170L, 180L, 190L, 200L)
        assertThat(remainingIds).containsAtLeast(100L, 110L, 120L, 130L, 140L, 150L)
    }

    // ========== Category 4: Single Message Edge Cases Tests ==========

    @Test
    fun topSizeLessThanNormalCount_messageIdInTop_shouldDeletePrevious() = runTest {
        // Arrange: Insert messages, messageId=100
        // Simulate topNearIds.size < normalCountTop and messageId exists in top
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 60, id = 60),
            createMessageEntity(tid = 70, id = 70),
            createMessageEntity(tid = 100, id = 100), // messageId - only top message returned
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150)
        )

        // Act: limit=10 → normalCountTop=5, but only return 1 top message (messageId itself)
        // This means topNearIds.size (1) < normalCountTop (5) and messageId is in topNearIds
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 100),
                createSceytMessage(id = 120),
                createSceytMessage(id = 130),
                createSceytMessage(id = 140),
                createSceytMessage(id = 150)
            ),
            syncStartTime = 0L
        )

        // Assert: deleteByLoadType(LoadPrev, includeMessage=true) called - deletes all ≤ 100
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(50L, 60L, 70L)
        assertThat(remainingIds).containsAtLeast(100L, 120L, 130L, 140L, 150L)
    }

    @Test
    fun bottomSizeLessThanNormalCount_messageIdInBottom_shouldDeleteNext() = runTest {
        // Arrange: Insert messages, messageId=150
        // Simulate bottomNearIds.size < normalCountBottom and messageId exists in bottom
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 60, id = 60),
            createMessageEntity(tid = 70, id = 70),
            createMessageEntity(tid = 80, id = 80),
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 150, id = 150), // messageId - only bottom message returned
            createMessageEntity(tid = 160, id = 160),
            createMessageEntity(tid = 170, id = 170),
            createMessageEntity(tid = 180, id = 180)
        )

        // Act: limit=10 → normalCountBottom=5, but only return 1 bottom message (messageId itself)
        // This means bottomNearIds.size (1) < normalCountBottom (5) and messageId is in bottomNearIds
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 50),
                createSceytMessage(id = 60),
                createSceytMessage(id = 70),
                createSceytMessage(id = 80),
                createSceytMessage(id = 90),
                createSceytMessage(id = 150)
            ),
            syncStartTime = 0L
        )

        // Assert: deleteByLoadType(LoadNext, includeMessage=true) called - deletes all ≥ 150 beyond returned
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(160L, 170L, 180L)
        assertThat(remainingIds).containsAtLeast(50L, 60L, 70L, 80L, 90L, 150L)
    }

    @Test
    fun topSizeLessThanNormalCount_messageIdNotInTop_shouldHandleInRange() = runTest {
        // Arrange: messageId not in top results
        // topNearIds.size < normalCountTop but messageId NOT in topNearIds
        insertMessages(
            createMessageEntity(tid = 80, id = 80),
            createMessageEntity(tid = 90, id = 90),
            createMessageEntity(tid = 95, id = 95), // Gap - will be deleted
            createMessageEntity(tid = 100, id = 100), // messageId (not returned)
            createMessageEntity(tid = 105, id = 105),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130)
        )

        // Act: limit=10 → normalCountTop=5
        // Return top=[80, 90] (size=2 < 5) and bottom=[105, 110, 120, 130]
        // messageId (100) not in returned messages, so it falls through to handleMessagesInRange
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 80),
                createSceytMessage(id = 90),
                createSceytMessage(id = 105),
                createSceytMessage(id = 110),
                createSceytMessage(id = 120),
                createSceytMessage(id = 130)
            ),
            syncStartTime = 0L
        )

        // Assert: Falls through to handleMessagesInRange at line 109
        // Should delete 95, 100 (gap between top and bottom)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(95L, 100L)
        assertThat(remainingIds).containsAtLeast(80L, 90L, 105L, 110L, 120L, 130L)
    }

    @Test
    fun bottomSizeLessThanNormalCount_messageIdNotInBottom_shouldHandleInRange() = runTest {
        // Arrange: messageId not in bottom results
        // bottomNearIds.size < normalCountBottom but messageId NOT in bottomNearIds
        insertMessages(
            createMessageEntity(tid = 50, id = 50),
            createMessageEntity(tid = 60, id = 60),
            createMessageEntity(tid = 70, id = 70),
            createMessageEntity(tid = 80, id = 80),
            createMessageEntity(tid = 95, id = 95),
            createMessageEntity(tid = 100, id = 100), // messageId (not returned)
            createMessageEntity(tid = 105, id = 105), // Gap - will be deleted
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120)
        )

        // Act: limit=10 → normalCountBottom=5
        // Return top=[50, 60, 70, 80, 95] and bottom=[110, 120] (size=2 < 5)
        // messageId (100) not in returned messages
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 50),
                createSceytMessage(id = 60),
                createSceytMessage(id = 70),
                createSceytMessage(id = 80),
                createSceytMessage(id = 95),
                createSceytMessage(id = 110),
                createSceytMessage(id = 120)
            ),
            syncStartTime = 0L
        )

        // Assert: Falls through to handleMessagesInRange
        // Should delete 100, 105 (gap between top and bottom)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsNoneOf(100L, 105L)
        assertThat(remainingIds).containsAtLeast(50L, 60L, 70L, 80L, 95L, 110L, 120L)
    }

    // ========== Category 5: Within Range Deletions Tests ==========

    @Test
    fun withinRange_shouldDeleteMissingMessages() = runTest {
        // Arrange: Insert messages 100, 110, 120, 130, 140, 150
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150)
        )

        // Act: Server returns [100, 130, 150] (missing 110, 120, 140)
        useCase(
            channelId = channelId,
            messageId = 125,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 100),
                createSceytMessage(id = 130),
                createSceytMessage(id = 150)
            ),
            syncStartTime = 0L
        )

        // Assert: Messages 110, 120, 140 deleted
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 130L, 150L)
        assertThat(remainingIds).containsNoneOf(110L, 120L, 140L)
    }

    @Test
    fun withinRange_allMessagesMatch_shouldDeleteNothing() = runTest {
        // Arrange: Insert messages 100, 110, 120
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120)
        )

        // Act: Server returns exact match
        useCase(
            channelId = channelId,
            messageId = 110,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 100),
                createSceytMessage(id = 110),
                createSceytMessage(id = 120)
            ),
            syncStartTime = 0L
        )

        // Assert: No deletions
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 110L, 120L)
    }

    @Test
    fun withinRange_shouldHandleUnsortedServerMessages() = runTest {
        // Arrange: Insert messages
        insertMessages(
            createMessageEntity(tid = 100, id = 100),
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150)
        )

        // Act: Server returns unsorted list [150, 100, 120] (missing 110, 130, 140)
        useCase(
            channelId = channelId,
            messageId = 125,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 150),
                createSceytMessage(id = 100),
                createSceytMessage(id = 120)
            ),
            syncStartTime = 0L
        )

        // Assert: Range calculated correctly (100-150), missing messages deleted
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 120L, 150L)
        assertThat(remainingIds).containsNoneOf(110L, 130L, 140L)
    }

    // ========== Category 6: Complex Scenarios Tests ==========

    @Test
    fun normalCountCalculation_evenLimit_shouldSplitCorrectly() = runTest {
        // Test: limit=50 → normalCountTop=25, normalCountBottom=25
        insertMessages(
            *(1..50).map { createMessageEntity(tid = it.toLong(), id = it.toLong()) }.toTypedArray()
        )

        // Act: Return 25 top + 25 bottom messages (exactly even split)
        val topMessages = (1..25).map { createSceytMessage(id = it.toLong()) }
        val bottomMessages = (26..50).map { createSceytMessage(id = it.toLong()) }

        useCase(
            channelId = channelId,
            messageId = 25,
            limit = 50,
            serverMessages = topMessages + bottomMessages,
            syncStartTime = 0L
        )

        // Assert: All messages remain (perfect split)
        val remainingCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Remaining count: $remainingCount")
        assertThat(remainingCount).isEqualTo(50)
    }

    @Test
    fun normalCountCalculation_oddLimit_shouldRoundUpTop() = runTest {
        // Test: limit=51 → normalCountTop=26 (rounded up), normalCountBottom=25
        insertMessages(
            *(1..51).map { createMessageEntity(tid = it.toLong(), id = it.toLong()) }.toTypedArray()
        )

        // Act: Return 26 top + 25 bottom messages (using roundUp)
        val topMessages = (1..26).map { createSceytMessage(id = it.toLong()) }
        val bottomMessages = (27..51).map { createSceytMessage(id = it.toLong()) }

        useCase(
            channelId = channelId,
            messageId = 26,
            limit = 51,
            serverMessages = topMessages + bottomMessages,
            syncStartTime = 0L
        )

        // Assert: All messages remain (roundUp logic works)
        val remainingCount = messageDao.getMessagesCount(channelId)
        Log.d("Test", "Remaining count: $remainingCount")
        assertThat(remainingCount).isEqualTo(51)
    }

    @Test
    fun messageIdAtBoundary_shouldHandleCorrectly() = runTest {
        // Arrange: messageId at start/end of range
        insertMessages(
            createMessageEntity(tid = 100, id = 100), // messageId at boundary
            createMessageEntity(tid = 110, id = 110),
            createMessageEntity(tid = 120, id = 120),
            createMessageEntity(tid = 130, id = 130),
            createMessageEntity(tid = 140, id = 140),
            createMessageEntity(tid = 150, id = 150)
        )

        // Act: Call with boundary messageId (100)
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = listOf(
                createSceytMessage(id = 100),
                createSceytMessage(id = 110),
                createSceytMessage(id = 120),
                createSceytMessage(id = 130),
                createSceytMessage(id = 140)
            ),
            syncStartTime = 0L
        )

        // Assert: Correct top/bottom split (top=[100], bottom=[110,120,130,140])
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining messages: $remainingIds")
        assertThat(remainingIds).containsAtLeast(100L, 110L, 120L, 130L, 140L)
    }

    // ========== syncStartTime Tests for LoadNear ==========

    @Test
    fun loadNear_emptyResponse_withSyncStartTime_shouldNotDeleteNewMessages() = runTest {
        // Arrange - Empty response means delete all, but respect syncStartTime
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 500),    // Old - should delete
            createMessageEntity(tid = 100, id = 100, createdAt = 900),  // Old - should delete
            createMessageEntity(tid = 150, id = 150, createdAt = 1100), // New - should NOT delete
            createMessageEntity(tid = 200, id = 200, createdAt = 1200)  // New - should NOT delete
        )

        val serverMessages = emptyList<SceytMessage>()

        // Act - Empty response with syncStartTime
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Should only delete old messages, keep new ones
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(150L, 200L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted (old)
        assertThat(remainingIds).doesNotContain(100L) // Deleted (old)
        assertThat(remainingIds).contains(150L)       // Kept (new)
        assertThat(remainingIds).contains(200L)       // Kept (new)
    }

    @Test
    fun loadNear_sizeLessThanLimit_withSyncStartTime_shouldNotDeleteNewMessages() = runTest {
        // Arrange - Complete list from server, but some local messages are new
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 500),    // Old, not in server - should delete
            createMessageEntity(tid = 100, id = 100, createdAt = 900),  // Old, in server - should keep
            createMessageEntity(tid = 150, id = 150, createdAt = 1100), // New, not in server - should NOT delete
            createMessageEntity(tid = 200, id = 200, createdAt = 200)   // Old, in server - should keep
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200)
        )

        // Act - size=2 < limit=10, so complete list
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Should delete old missing messages (50), but keep new ones (150)
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining after size < limit with syncStartTime: $remainingIds")
        assertThat(remainingIds).containsExactly(100L, 150L, 200L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted (old, not in server)
        assertThat(remainingIds).contains(100L)       // Kept (in server)
        assertThat(remainingIds).contains(150L)       // Kept (new, even though not in server)
        assertThat(remainingIds).contains(200L)       // Kept (in server)
    }

    @Test
    fun loadNear_topReachedEnd_withSyncStartTime_shouldNotDeleteNewMessages() = runTest {
        // Arrange - Reached end in top direction, with new messages
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 500),    // Old, before returned range - should delete
            createMessageEntity(tid = 75, id = 75, createdAt = 1100),   // New, before returned range - should NOT delete
            createMessageEntity(tid = 100, id = 100, createdAt = 100),  // In range (top)
            createMessageEntity(tid = 110, id = 110, createdAt = 110),  // In range (bottom)
            createMessageEntity(tid = 120, id = 120, createdAt = 120)   // In range (bottom)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),  // Top message (messageId)
            createSceytMessage(id = 110),
            createSceytMessage(id = 120)
        )

        // Act - Top count=1 < normalCountTop=2 (limit=5), so reached end in top direction
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 5,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Should delete old messages before 100, but keep new ones
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining after top reached end with syncStartTime: $remainingIds")
        assertThat(remainingIds).containsExactly(75L, 100L, 110L, 120L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted (old, before range)
        assertThat(remainingIds).contains(75L)        // Kept (new, even though before returned range)
    }

    @Test
    fun loadNear_bottomReachedEnd_withSyncStartTime_shouldNotDeleteNewMessages() = runTest {
        // Arrange - Reached end in bottom direction, with new messages
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 90, id = 90, createdAt = 90),     // In range (top)
            createMessageEntity(tid = 100, id = 100, createdAt = 100),  // In range (messageId)
            createMessageEntity(tid = 110, id = 110, createdAt = 110),  // In range (bottom)
            createMessageEntity(tid = 200, id = 200, createdAt = 900),  // Old, after returned range - should delete
            createMessageEntity(tid = 300, id = 300, createdAt = 1100)  // New, after returned range - should NOT delete
        )

        val serverMessages = listOf(
            createSceytMessage(id = 90),
            createSceytMessage(id = 100),  // messageId
            createSceytMessage(id = 110)   // Bottom message
        )

        // Act - Bottom count=1 < normalCountBottom=2 (limit=5), so reached end in bottom direction
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 5,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Should delete old messages after 110, but keep new ones
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining after bottom reached end with syncStartTime: $remainingIds")
        assertThat(remainingIds).containsExactly(90L, 100L, 110L, 300L)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (old, after range)
        assertThat(remainingIds).contains(300L)       // Kept (new, even though after returned range)
    }

    @Test
    fun loadNear_noTopMessages_withSyncStartTime_shouldRespectTimeBoundary() = runTest {
        // Arrange - No top messages found, with new messages before bottom range
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 500),    // Old, before returned - should delete
            createMessageEntity(tid = 75, id = 75, createdAt = 1100),   // New, before returned - should NOT delete
            createMessageEntity(tid = 100, id = 100, createdAt = 100),  // Returned (bottom)
            createMessageEntity(tid = 110, id = 110, createdAt = 110)   // Returned (bottom)
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 110)
        )

        // Act - No top messages (all returned > messageId=100)
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(75L, 100L, 110L)
        assertThat(remainingIds).doesNotContain(50L) // Deleted (old)
        assertThat(remainingIds).contains(75L)       // Kept (new)
    }

    @Test
    fun loadNear_noBottomMessages_withSyncStartTime_shouldRespectTimeBoundary() = runTest {
        // Arrange - No bottom messages found, with new messages after top range
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 90, id = 90, createdAt = 90),     // Returned (top)
            createMessageEntity(tid = 100, id = 100, createdAt = 100),  // Returned (top)
            createMessageEntity(tid = 200, id = 200, createdAt = 900),  // Old, after returned - should delete
            createMessageEntity(tid = 300, id = 300, createdAt = 1100)  // New, after returned - should NOT delete
        )

        val serverMessages = listOf(
            createSceytMessage(id = 90),
            createSceytMessage(id = 100)
        )

        // Act - No bottom messages (all returned <= messageId=100)
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 30,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(90L, 100L, 300L)
        assertThat(remainingIds).doesNotContain(200L) // Deleted (old)
        assertThat(remainingIds).contains(300L)       // Kept (new)
    }

    @Test
    fun loadNear_withinRange_withSyncStartTime_shouldRespectTimeBoundary() = runTest {
        // Arrange - Within range deletions DO use syncStartTime
        // Even gap detection within LoadNear should preserve new messages
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 100, id = 100, createdAt = 100),   // Returned
            createMessageEntity(tid = 125, id = 125, createdAt = 1100),  // Gap, NEW - should KEEP
            createMessageEntity(tid = 150, id = 150, createdAt = 900),   // Gap, OLD - should DELETE
            createMessageEntity(tid = 200, id = 200, createdAt = 200)    // Returned
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),
            createSceytMessage(id = 200)
        )

        // Act - Normal LoadNear, size == limit (no "reached end")
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 2,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Within-range check respects syncStartTime
        // 150 deleted (old), 125 kept (new)
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(100L, 125L, 200L)
        assertThat(remainingIds).contains(125L)       // Kept (new, created after syncStartTime)
        assertThat(remainingIds).doesNotContain(150L) // Deleted (old, created before syncStartTime)
    }

    @Test
    fun loadNear_complexScenario_withSyncStartTime_shouldHandleAllCases() = runTest {
        // Arrange - Complex scenario combining multiple deletion types
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 10, id = 10, createdAt = 500),    // Old, before returned range - DELETE
            createMessageEntity(tid = 20, id = 20, createdAt = 1100),   // New, before returned range - KEEP
            createMessageEntity(tid = 100, id = 100, createdAt = 100),  // In range, returned - KEEP
            createMessageEntity(tid = 125, id = 125, createdAt = 900),  // In range, missing - DELETE
            createMessageEntity(tid = 150, id = 150, createdAt = 1200), // In range, missing - DELETE (within range)
            createMessageEntity(tid = 200, id = 200, createdAt = 200),  // In range, returned - KEEP
            createMessageEntity(tid = 300, id = 300, createdAt = 800),  // Old, after returned range - DELETE
            createMessageEntity(tid = 400, id = 400, createdAt = 1300)  // New, after returned range - KEEP
        )

        val serverMessages = listOf(
            createSceytMessage(id = 100),  // Top
            createSceytMessage(id = 200)   // Bottom
        )

        // Act - size=2 < limit=10, complete list
        useCase(
            channelId = channelId,
            messageId = 150,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert
        val remainingIds = messageDao.getMessagesIds(channelId)
        Log.d("Test", "Remaining after complex scenario: $remainingIds")
        assertThat(remainingIds).containsExactly(20L, 100L, 150L, 200L, 400L)
        assertThat(remainingIds).doesNotContain(10L)  // Deleted (old, not in complete list)
        assertThat(remainingIds).contains(20L)        // Kept (new, even though not in server)
        assertThat(remainingIds).contains(100L)       // Kept (in server)
        assertThat(remainingIds).doesNotContain(125L) // Deleted (old, not in complete list)
        assertThat(remainingIds).contains(150L)       // Kept (new, even though not in server)
        assertThat(remainingIds).contains(200L)       // Kept (in server)
        assertThat(remainingIds).doesNotContain(300L) // Deleted (old, not in complete list)
        assertThat(remainingIds).contains(400L)       // Kept (new, even though not in server)
    }

    @Test
    fun loadNear_boundaryTimestamp_shouldKeepMessagesAtOrAfterSyncStartTime() = runTest {
        // Arrange - Test boundary condition: messages at exact syncStartTime
        val syncStartTime = 1000L
        insertMessages(
            createMessageEntity(tid = 50, id = 50, createdAt = 999),    // Before - DELETE
            createMessageEntity(tid = 75, id = 75, createdAt = 1000),   // AT boundary - KEEP (>= not >)
            createMessageEntity(tid = 100, id = 100, createdAt = 1001), // After - KEEP
            createMessageEntity(tid = 200, id = 200, createdAt = 200)   // Returned
        )

        val serverMessages = listOf(
            createSceytMessage(id = 200)
        )

        // Act - size=1 < limit=10 (complete list)
        useCase(
            channelId = channelId,
            messageId = 100,
            limit = 10,
            serverMessages = serverMessages,
            syncStartTime = syncStartTime
        )

        // Assert - Messages with createdAt < syncStartTime deleted, >= syncStartTime kept
        // Condition is `createdAt < syncStartTime`, so message at exactly 1000 is KEPT
        val remainingIds = messageDao.getMessagesIds(channelId)
        assertThat(remainingIds).containsExactly(75L, 100L, 200L)
        assertThat(remainingIds).doesNotContain(50L)  // Deleted (createdAt=999 < syncStartTime=1000)
        assertThat(remainingIds).contains(75L)        // Kept (createdAt=1000 >= syncStartTime=1000)
        assertThat(remainingIds).contains(100L)       // Kept (createdAt=1001 > syncStartTime=1000)
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

