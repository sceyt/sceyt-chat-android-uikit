package com.sceyt.chatuikit.persistence.dao.reactiondaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionTotalEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ReactionDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var reactionDao: ReactionDao
    private lateinit var messageDao: MessageDao
    private lateinit var userDao: UserDao

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
        reactionDao = database.reactionDao()
        messageDao = database.messageDao()
        userDao = database.userDao()
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
    ) = MessageEntity(
        tid = tid,
        id = id,
        channelId = channelId,
        body = "body",
        type = "text",
        metadata = null,
        createdAt = tid,
        updatedAt = 0,
        incoming = false,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = MessageDeliveryStatus.Displayed,
        state = MessageState.Unmodified,
        fromId = "user1",
        markerCount = null,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        disableMentionsCount = false,
        unList = false,
    )

    private fun user(
        id: String,
        firstName: String = "First$id",
        lastName: String = "Last$id",
    ) = UserEntity(
        id = id,
        username = id,
        firstName = firstName,
        lastName = lastName,
        avatarURL = null,
        presence = SceytPresence(PresenceState.Online, "online", 0L),
        activityStatus = UserState.Active,
        blocked = false,
    )

    private fun userDb(entity: UserEntity) = UserDb(entity, emptyList())

    private fun reaction(
        id: Long,
        messageId: Long,
        key: String = "like",
        score: Int = 1,
        fromId: String,
        createdAt: Long = id,
    ) = ReactionEntity(
        id = id,
        messageId = messageId,
        key = key,
        score = score,
        reason = "reason_$id",
        createdAt = createdAt,
        fromId = fromId,
    )

    private fun total(
        messageId: Long,
        key: String,
        score: Int,
        count: Long,
    ) = ReactionTotalEntity(
        messageId = messageId,
        key = key,
        score = score,
        count = count,
    )

    private suspend fun insertMessages(vararg messages: MessageEntity) {
        messageDao.upsertMessageEntitiesWithTransaction(messages.toList())
    }

    private suspend fun insertUsers(vararg users: UserEntity) {
        userDao.insertUsersWithMetadata(users.map(::userDb))
    }

    // endregion

    @Test
    fun insertReactionsIfMessageExist_insertsOnlyForExistingMessages() = runTest {
        insertMessages(message(tid = 10, id = 10))

        reactionDao.insertReactionsIfMessageExist(
            listOf(
                reaction(id = 1, messageId = 10, fromId = "user1"),
                reaction(id = 2, messageId = 999, fromId = "user2"),
            )
        )

        assertThat(reactionDao.getReactionsByMsgId(10L).map { it.reaction.id }).containsExactly(1L)
        assertThat(reactionDao.getReactionsByMsgId(999L)).isEmpty()
    }

    @Test
    fun insertMessageReactionsAndTotalsIfMessageExist_insertsRowsWhenMessageExists() = runTest {
        insertMessages(message(tid = 10, id = 10))

        reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
            messageId = 10L,
            reactions = listOf(reaction(id = 1, messageId = 10, fromId = "user1")),
            reactionTotals = listOf(total(messageId = 10, key = "like", score = 1, count = 1)),
        )

        assertThat(reactionDao.getReactionsByMsgIdAndKey(10L, "like").map { it.reaction.id }).containsExactly(1L)
        assertThat(reactionDao.getReactionTotal(10L, "like")!!.score).isEqualTo(1)
    }

    @Test
    fun insertReactionAndIncreaseTotalIfNeeded_createsTotalForFirstReaction() = runTest {
        insertMessages(message(tid = 10, id = 10))

        reactionDao.insertReactionAndIncreaseTotalIfNeeded(
            reaction(id = 1, messageId = 10, key = "wow", score = 2, fromId = "user1")
        )

        val total = reactionDao.getReactionTotal(10L, "wow")
        assertThat(total).isNotNull()
        assertThat(total!!.score).isEqualTo(2)
        assertThat(total.count).isEqualTo(1)
        assertThat(reactionDao.getUserReactionByKey(10L, "user1", "wow")!!.reaction.id).isEqualTo(1L)
    }

    @Test
    fun insertReactionAndIncreaseTotalIfNeeded_doesNotIncreaseTotalWhenReactionAlreadyExists() = runTest {
        insertMessages(message(tid = 10, id = 10))

        reactionDao.insertReactionAndIncreaseTotalIfNeeded(
            reaction(id = 1, messageId = 10, key = "wow", score = 1, fromId = "user1")
        )
        reactionDao.insertReactionAndIncreaseTotalIfNeeded(
            reaction(id = 2, messageId = 10, key = "wow", score = 1, fromId = "user1")
        )

        val total = reactionDao.getReactionTotal(10L, "wow")
        assertThat(total).isNotNull()
        assertThat(total!!.score).isEqualTo(1)
        assertThat(reactionDao.getReactionsByMsgId(10L).map { it.reaction.id }).containsExactly(2L)
    }

    @Test
    fun getReactionsById_returnsReactionWithJoinedUser() = runTest {
        insertMessages(message(tid = 10, id = 10))
        insertUsers(user("user1", firstName = "Alice"))
        reactionDao.insertReactionsIfMessageExist(listOf(reaction(id = 1, messageId = 10, fromId = "user1")))

        val result = reactionDao.getReactionsById(1L)

        assertThat(result).isNotNull()
        val from = checkNotNull(result!!.from)
        assertThat(from.user.id).isEqualTo("user1")
        assertThat(from.user.firstName).isEqualTo("Alice")
    }

    @Test
    fun getReactions_returnsPagedDescendingById() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertReactionsIfMessageExist(
            listOf(
                reaction(id = 1, messageId = 10, fromId = "user1"),
                reaction(id = 2, messageId = 10, fromId = "user2"),
                reaction(id = 3, messageId = 10, fromId = "user3"),
            )
        )

        val result = reactionDao.getReactions(messageId = 10L, limit = 2, offset = 1)

        assertThat(result.map { it.reaction.id }).isEqualTo(listOf(2L, 1L))
    }

    @Test
    fun getReactionsByKey_returnsPagedMatchingRows() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertReactionsIfMessageExist(
            listOf(
                reaction(id = 1, messageId = 10, key = "like", fromId = "user1"),
                reaction(id = 2, messageId = 10, key = "love", fromId = "user2"),
                reaction(id = 3, messageId = 10, key = "like", fromId = "user3"),
            )
        )

        val result = reactionDao.getReactionsByKey(messageId = 10L, limit = 1, offset = 1, key = "like")

        assertThat(result.map { it.reaction.id }).containsExactly(1L)
    }

    @Test
    fun getSelfReactionsByMessageId_returnsOnlyRequestedUsersReactions() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertReactionsIfMessageExist(
            listOf(
                reaction(id = 1, messageId = 10, fromId = "user1"),
                reaction(id = 2, messageId = 10, fromId = "user2"),
            )
        )

        val result = reactionDao.getSelfReactionsByMessageId(10L, "user1")

        assertThat(result.map { it.reaction.id }).containsExactly(1L)
    }

    @Test
    fun deleteReactionAndTotal_decrementsTotalWhenOtherRowsRemain() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
            messageId = 10L,
            reactions = listOf(
                reaction(id = 1, messageId = 10, fromId = "user1"),
                reaction(id = 2, messageId = 10, fromId = "user2"),
            ),
            reactionTotals = listOf(total(messageId = 10, key = "like", score = 2, count = 2)),
        )

        reactionDao.deleteReactionAndTotal(messageId = 10L, key = "like", fromId = "user1", score = 1)

        val total = reactionDao.getReactionTotal(10L, "like")
        assertThat(total).isNotNull()
        assertThat(total!!.score).isEqualTo(1)
        assertThat(reactionDao.getReactionsByMsgId(10L).map { it.reaction.fromId }).containsExactly("user2")
    }

    @Test
    fun deleteReactionAndTotal_deletesTotalWhenLastReactionRemoved() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
            messageId = 10L,
            reactions = listOf(reaction(id = 1, messageId = 10, fromId = "user1")),
            reactionTotals = listOf(total(messageId = 10, key = "like", score = 1, count = 1)),
        )

        reactionDao.deleteReactionAndTotal(messageId = 10L, key = "like", fromId = "user1", score = 1)

        assertThat(reactionDao.getReactionTotal(10L, "like")).isNull()
        assertThat(reactionDao.getReactionsByMsgId(10L)).isEmpty()
    }

    @Test
    fun deleteAllReactionsAndTotals_clearsBothTablesForMessage() = runTest {
        insertMessages(message(tid = 10, id = 10))
        reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
            messageId = 10L,
            reactions = listOf(
                reaction(id = 1, messageId = 10, fromId = "user1"),
                reaction(id = 2, messageId = 10, key = "love", fromId = "user2"),
            ),
            reactionTotals = listOf(
                total(messageId = 10, key = "like", score = 1, count = 1),
                total(messageId = 10, key = "love", score = 1, count = 1),
            ),
        )

        reactionDao.deleteAllReactionsAndTotals(10L)

        assertThat(reactionDao.getReactionsByMsgId(10L)).isEmpty()
        assertThat(reactionDao.getReactionTotal(10L, "like")).isNull()
        assertThat(reactionDao.getReactionTotal(10L, "love")).isNull()
    }
}
