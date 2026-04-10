package com.sceyt.chatuikit.persistence.dao.searchdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SearchMessageDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var channelDao: ChannelDao
    private lateinit var messageDao: MessageDao
    private lateinit var globalSearchDao: GlobalSearchDao

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
        channelDao = database.channelDao()
        messageDao = database.messageDao()
        globalSearchDao = database.globalSearchDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region searchMessagesGlobally — filtering

    @Test
    fun searchMessagesGlobally_singleWordQuery_returnsMatchingMessages() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello world", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "goodbye world", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="hello", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_singleWordQuery_returnsNothingWhenNoMatch() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello world", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="missing", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun searchMessagesGlobally_blankQuery_returnsAllNonPendingNonUnlistedMessages() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "alpha", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "beta", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "pending", createdAt = 300, deliveryStatus = MessageDeliveryStatus.Pending),
            message(tid = 4, id = 4, channelId = 1, body = "unlisted", createdAt = 400, unList = true),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_filtersBySender() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello from alice", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "hello from bob", fromId = "bob", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "hello",
            senderId = "alice",
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_nullSender_returnsAllSenders() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello from alice", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "hello from bob", fromId = "bob", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="hello", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_excludesPendingMessages() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "delivered", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "pending message", createdAt = 200, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_excludesUnlistedMessages() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "listed", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "unlisted message", createdAt = 200, unList = true),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_excludesMessagesWithNullId() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = null, channelId = 1, body = "local only message", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "synced message", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L)
    }

    // endregion

    // region searchMessagesGlobally — ordering

    @Test
    fun searchMessagesGlobally_orderedByCreatedAtDescending() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "first", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "second", createdAt = 300),
            message(tid = 3, id = 3, channelId = 1, body = "third", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_sameCreatedAt_orderedByMessageIdDescending() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 10, channelId = 1, body = "msg a", createdAt = 500),
            message(tid = 2, id = 30, channelId = 1, body = "msg b", createdAt = 500),
            message(tid = 3, id = 20, channelId = 1, body = "msg c", createdAt = 500),
        )

        val result = globalSearchDao.searchMessages(
            query ="", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(30L, 20L, 10L).inOrder()
    }

    // endregion

    // region searchMessagesGlobally — pagination

    @Test
    fun searchMessagesGlobally_limitRestrictsResultCount() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "msg", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query ="msg", senderId = null, limit = 2, offset = 0)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.messageEntity.id }).containsExactly(3L, 2L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_offsetSkipsResults() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "msg", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query ="msg", senderId = null, limit = 10, offset = 1)

        // Ordered: 3, 2, 1 — skipping first 1 → [2, 1]
        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    // endregion

    // region searchMessagesGlobally — query matching

    @Test
    fun searchMessagesGlobally_caseInsensitiveMatch() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "Hello World", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="hello", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_leadingSpacesTrimmed() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello world", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="   hello", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_trailingSpacesTrimmed() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello world", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="hello   ", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_leadingAndTrailingSpacesTrimmed() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello world", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="  hello  ", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multiWordQuery_allWordsMustBePresent() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "meeting at noon today", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "meeting tomorrow", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="meeting noon", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multiWordQuery_noResultWhenAnyWordMissing() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "meeting at noon", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="meeting missing", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun searchMessagesGlobally_multiWordQuery_wordsOrderIndependent() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "alice sent to bob", createdAt = 100),
        )

        // words in opposite order to body
        val result = globalSearchDao.searchMessages(
            query ="bob alice", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multiWordQuery_caseInsensitive() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "Hello World", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query ="HELLO world", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multipleInternalSpacesSplitIntoWords() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "alpha beta gamma", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "alpha only", createdAt = 200),
        )

        // "alpha" and "gamma" with extra spaces between them
        val result = globalSearchDao.searchMessages(
            query ="alpha   gamma", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_partialWordMatch() = runTest {
        insertChannels(channel(id = 1))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "retrospective meeting", createdAt = 100),
        )

        // partial word should still match via LIKE '%..%'
        val result = globalSearchDao.searchMessages(
            query ="retro", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_matchesAcrossMultipleChannels() = runTest {
        insertChannels(
            channel(id = 1, type = ChannelTypeEnum.Group.value),
            channel(id = 2, type = ChannelTypeEnum.Direct.value),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "launch plan", createdAt = 100),
            message(tid = 2, id = 2, channelId = 2, body = "launch schedule", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query ="launch", senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Direct.value, ChannelTypeEnum.Group.value),
            onlyJoined = false,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    // endregion

    // region searchMessagesInBroadcastJoinedChannels — channel type filtering

    @Test
    fun searchMessagesInBroadcastJoinedChannels_onlyReturnsBroadcastTypeChannels() = runTest {
        insertChannels(
            channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"),   // broadcast — include
            channel(id = 2, type = ChannelTypeEnum.Group.value, userRole = "member"),    // group — exclude
            channel(id = 3, type = ChannelTypeEnum.Direct.value, userRole = "member"),  // direct — exclude
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "broadcast msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 2, body = "group msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 3, body = "direct msg", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_excludesNullUserRole() = runTest {
        insertChannels(
            channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"),   // joined — include
            channel(id = 2, type = ChannelTypeEnum.Public.value, userRole = null),       // not joined — exclude
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "joined msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 2, body = "not joined msg", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_excludesEmptyUserRole() = runTest {
        insertChannels(
            channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "owner"),   // joined — include
            channel(id = 2, type = ChannelTypeEnum.Public.value, userRole = ""),        // empty role — exclude
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "joined msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 2, body = "empty role msg", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_includesAnyNonEmptyUserRole() = runTest {
        insertChannels(
            channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "owner"),
            channel(id = 2, type = ChannelTypeEnum.Public.value, userRole = "member"),
            channel(id = 3, type = ChannelTypeEnum.Public.value, userRole = "admin"),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "owner msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 2, body = "member msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 3, body = "admin msg", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(3L, 2L, 1L).inOrder()
    }

    // endregion

    // region searchMessagesInBroadcastJoinedChannels — filtering

    @Test
    fun searchMessagesInBroadcastJoinedChannels_excludesPendingMessages() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "delivered", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "pending", createdAt = 200, deliveryStatus = MessageDeliveryStatus.Pending),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_excludesUnlistedMessages() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "listed", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "unlisted", createdAt = 200, unList = true),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_excludesMessagesWithNullId() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = null, channelId = 1, body = "local only", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "synced", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_filtersBySender() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello from alice", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "hello from bob", fromId = "bob", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "hello",
            senderId = "alice",
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_nullSender_returnsAllSenders() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello from alice", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "hello from bob", fromId = "bob", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "hello",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_senderFilterCombinedWithQuery() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "release notes", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "release notes", fromId = "bob", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "other content", fromId = "alice", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query = "release",
            senderId = "alice",
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    // endregion

    // region searchMessagesInBroadcastJoinedChannels — ordering

    @Test
    fun searchMessagesInBroadcastJoinedChannels_orderedByCreatedAtDescending() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "first", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "third", createdAt = 300),
            message(tid = 3, id = 3, channelId = 1, body = "second", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_sameCreatedAt_orderedByMessageIdDescending() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 10, channelId = 1, body = "a", createdAt = 500),
            message(tid = 2, id = 30, channelId = 1, body = "b", createdAt = 500),
            message(tid = 3, id = 20, channelId = 1, body = "c", createdAt = 500),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(30L, 20L, 10L).inOrder()
    }

    // endregion

    // region searchMessagesInBroadcastJoinedChannels — pagination

    @Test
    fun searchMessagesInBroadcastJoinedChannels_limitRestrictsResultCount() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "msg", createdAt = 300),
        )

        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 2,
            offset = 0,
        )

        assertThat(result).hasSize(2)
        assertThat(result.map { it.messageEntity.id }).containsExactly(3L, 2L).inOrder()
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_offsetSkipsResults() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "msg", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "msg", createdAt = 200),
            message(tid = 3, id = 3, channelId = 1, body = "msg", createdAt = 300),
        )

        // Ordered: 3, 2, 1 — skipping first 1 → [2, 1]
        val result = globalSearchDao.searchMessages(
            query = "msg",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 10,
            offset = 1,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    // endregion

    // region searchMessagesInBroadcastJoinedChannels — query matching

    @Test
    fun searchMessagesInBroadcastJoinedChannels_blankQuery_returnsAllEligibleMessages() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "any content here", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "different body", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_singleWordQuery_matchesContainingMessages() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "release notes are here", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "no match body", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "release",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_caseInsensitiveMatch() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "Release Notes", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query = "release",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_multiWordQuery_allWordsMustBePresent() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "new release notes available", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "new notes only", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "release notes",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_multiWordQuery_noResultWhenAnyWordMissing() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "release notes", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query = "release missing",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_multiWordQuery_wordsOrderIndependent() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "alpha beta gamma", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query = "gamma alpha",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_multipleInternalSpacesSplitIntoWords() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "alpha gamma", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "alpha only", createdAt = 200),
        )

        val result = globalSearchDao.searchMessages(
            query = "alpha   gamma",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_leadingAndTrailingSpacesTrimmed() = runTest {
        insertChannels(channel(id = 1, type = ChannelTypeEnum.Public.value, userRole = "member"))
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "release notes", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query = "  release  ",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesInBroadcastJoinedChannels_emptyResults_whenNoChannelsMatch() = runTest {
        // No channels inserted — nothing to match
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "orphan message", createdAt = 100),
        )

        val result = globalSearchDao.searchMessages(
            query = "orphan",
            senderId = null,
            channelTypes = listOf(ChannelTypeEnum.Public.value),
            onlyJoined = true,
            limit = 20,
            offset = 0,
        )

        assertThat(result).isEmpty()
    }

    // endregion

    // region helpers

    private suspend fun insertChannels(vararg channels: ChannelEntity) {
        channelDao.insertChannelsAndLinks(channels.toList(), emptyList())
    }

    private suspend fun insertMessages(vararg messages: MessageEntity) {
        messageDao.upsertMessageEntitiesWithTransaction(messages.toList())
    }

    private fun channel(
        id: Long,
        type: String = ChannelTypeEnum.Direct.value,
        userRole: String? = "owner",
    ) = ChannelEntity(
        id = id,
        parentChannelId = null,
        uri = "channel://$id",
        type = type,
        subject = "channel $id",
        avatarUrl = null,
        metadata = null,
        createdAt = id,
        updatedAt = 0,
        messagesClearedAt = 0,
        memberCount = 0,
        createdById = null,
        userRole = userRole,
        unread = false,
        newMessageCount = 0,
        newMentionCount = 0,
        newReactedMessageCount = 0,
        hidden = false,
        archived = false,
        muted = false,
        mutedTill = null,
        pinnedAt = null,
        lastReceivedMessageId = 0,
        lastDisplayedMessageId = 0,
        messageRetentionPeriod = 0,
        lastMessageTid = id,
        lastMessageAt = id,
        pending = false,
        isSelf = false,
    )

    private fun message(
        tid: Long,
        id: Long?,
        channelId: Long,
        body: String,
        fromId: String = "user",
        createdAt: Long,
        deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Displayed,
        unList: Boolean = false,
    ) = MessageEntity(
        tid = tid,
        id = id,
        channelId = channelId,
        body = body,
        type = "text",
        metadata = null,
        createdAt = createdAt,
        updatedAt = 0,
        incoming = false,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = deliveryStatus,
        state = MessageState.Unmodified,
        fromId = fromId,
        markerCount = null,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        disableMentionsCount = false,
        unList = unList,
    )

    // endregion
}
