package com.sceyt.chatuikit.persistence.dao

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
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PinnedMessageDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var pinnedMessageDao: PinnedMessageDao

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
        pinnedMessageDao = database.pinnedMessageDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun ordersByServerPinIdNotByMessageDate() = runTest {
        // Pin ids deliberately assigned against timeline order, so the ordering can only be
        // right if the query reads serverPinId rather than the message's createdAt.
        insertMessage(tid = 1L, createdAt = 300L)
        insertMessage(tid = 2L, createdAt = 100L)
        insertMessage(tid = 3L, createdAt = 200L)
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 1L, serverPinId = 300L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 2L, serverPinId = 100L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 3L, serverPinId = 200L))

        val tids = pinnedMessageDao.getPinnedMessagesFlow(CHANNEL_ID, NOW).first()
            .map { it.pinnedMessageEntity.messageTid }

        assertThat(tids).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun tiesInPinIdsFallBackToTimelineOrder() = runTest {
        insertMessage(tid = 1L, createdAt = 300L)
        insertMessage(tid = 2L, createdAt = 100L)
        insertMessage(tid = 3L, createdAt = 200L)
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 1L, serverPinId = 0L, messageCreatedAt = 300L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 2L, serverPinId = 0L, messageCreatedAt = 100L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 3L, serverPinId = 0L, messageCreatedAt = 200L))

        val legacyTids = pinnedMessageDao.getPinnedMessagesFlow(CHANNEL_ID, NOW).first()
            .map { it.pinnedMessageEntity.messageTid }

        assertThat(legacyTids).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun deletingTheMessageCascadesToThePin() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 2L, serverPinId = 2L))

        messageDao.deleteMessageByTid(1L)

        assertThat(pinnedMessageDao.getByTid(1L)).isNull()
        assertThat(pinnedMessageDao.getByTid(2L)).isNotNull()
    }

    @Test
    fun aPinForAMessageThatIsNotStoredIsSkippedRatherThanBreakingTheTransaction() = runTest {
        // The foreign key to the message is deferred, so this pin would not fail here — it
        // would bring the whole transaction down at commit, taking unrelated writes with it.
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 404L, serverPinId = 1L))

        assertThat(pinnedMessageDao.getPinnedMessagesFlow(CHANNEL_ID, NOW).first()).isEmpty()
    }

    @Test
    fun reconcileCandidatesAreThePinsTheServerDidNotReport() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 2L, serverPinId = 2L))

        val stale = pinnedMessageDao.getExcluding(CHANNEL_ID, listOf(2L))

        assertThat(stale.map { it.messageTid }).containsExactly(1L)
    }

    @Test
    fun deleteByTidsRemovesOnlyThoseConfirmedPins() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.insertIfMessageExists(pin(messageTid = 2L, serverPinId = 2L))

        pinnedMessageDao.deleteByTids(listOf(1L))

        assertThat(pinnedMessageDao.getByTid(1L)).isNull()
        assertThat(pinnedMessageDao.getByTid(2L)).isNotNull()
    }

    @Test
    fun insertAllSkipsPinsOfMessagesThatAreNotStored() = runTest {
        insertMessage(tid = 1L)

        pinnedMessageDao.insertAllIfMessagesExist(
            listOf(pin(messageTid = 1L, serverPinId = 1L), pin(messageTid = 404L, serverPinId = 2L))
        )

        assertThat(pinnedMessageDao.getPinnedMessagesFlow(CHANNEL_ID, NOW).first().map { it.pinnedMessageEntity.messageTid })
            .containsExactly(1L)
    }

    @Test
    fun aMessageShowsItsConfirmedPinScope() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.insertIfMessageExists(
            pin(messageTid = 1L, serverPinId = 1L, pinScope = StoredPinScope.ForMe.value)
        )

        assertThat(messageDao.getMessageByTid(1L)?.pinnedMessage?.pinScope)
            .isEqualTo(StoredPinScope.ForMe.value)
    }

    @Test
    fun anExpiredPinIsExcludedFromDisplay() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.insertIfMessageExists(
            pin(messageTid = 1L, serverPinId = 1L, pinnedUntil = NOW - 1L)
        )

        assertThat(pinnedMessageDao.getPinnedMessagesFlow(CHANNEL_ID, NOW).first()).isEmpty()
    }

    private suspend fun insertMessage(tid: Long, createdAt: Long = tid) {
        messageDao.upsertMessage(
            MessageDb(
                messageEntity = MessageEntity(
                    tid = tid,
                    id = tid,
                    channelId = CHANNEL_ID,
                    body = "body",
                    type = "text",
                    metadata = null,
                    createdAt = createdAt,
                    updatedAt = 0L,
                    incoming = false,
                    isTransient = false,
                    silent = false,
                    viewOnce = false,
                    deliveryStatus = MessageDeliveryStatus.Displayed,
                    state = MessageState.Unmodified,
                    fromId = null,
                    markerCount = null,
                    mentionedUsersIds = null,
                    parentId = null,
                    replyCount = 0L,
                    displayCount = 0,
                    autoDeleteAt = null,
                    forwardingDetailsDb = null,
                    bodyAttribute = null,
                    disableMentionsCount = false,
                    unList = false,
                ),
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
                pinnedMessage = null,
                pendingPin = null,
            )
        )
    }

    private fun pin(
        messageTid: Long,
        serverPinId: Long,
        pinnedUntil: Long? = null,
        pinScope: Int = StoredPinScope.ForAll.value,
        messageCreatedAt: Long = messageTid,
    ) = PinnedMessageEntity(
        messageTid = messageTid,
        channelId = CHANNEL_ID,
        messageId = messageTid,
        pinScope = pinScope,
        pinnedAt = 0L,
        pinnedUntil = pinnedUntil,
        pinnedByUserId = null,
        messageCreatedAt = messageCreatedAt,
        serverPinId = serverPinId,
    )

    private companion object {
        const val CHANNEL_ID = 1L
        const val NOW = 10_000L
    }
}
