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
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.data.models.messages.PinSyncStates
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity.Companion.UNKNOWN_SERVER_PIN_ID
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
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 300L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 2L, serverPinId = 100L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 3L, serverPinId = 200L))

        val tids = pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)
            .map { it.pinnedMessageEntity.messageTid }

        assertThat(tids).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun anOptimisticPinSortsToTheNewestEnd() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 5L))
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 2L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN
            )
        )

        val tids = pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)
            .map { it.pinnedMessageEntity.messageTid }

        // Appends rather than jumping to the head, so existing banner ordinals do not shift.
        assertThat(tids).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun tiesInOptimisticAndLegacyPinIdsFallBackToTimelineOrder() = runTest {
        insertMessage(tid = 1L, createdAt = 300L)
        insertMessage(tid = 2L, createdAt = 100L)
        insertMessage(tid = 3L, createdAt = 200L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 0L, messageCreatedAt = 300L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 2L, serverPinId = 0L, messageCreatedAt = 100L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 3L, serverPinId = 0L, messageCreatedAt = 200L))

        val legacyTids = pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)
            .map { it.pinnedMessageEntity.messageTid }

        assertThat(legacyTids).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun pendingIntentsIncludeBothDirectionsOldestAttemptFirst() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        insertMessage(tid = 3L)
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 1L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN,
                lastAttemptAt = 3_000L,
            )
        )
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 2L,
                serverPinId = 2L,
                syncState = PinSyncStates.PENDING_UNPIN,
                lastAttemptAt = 2_000L,
            )
        )
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 3L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN,
                lastAttemptAt = 1_000L,
            )
        )

        val pending = pinnedMessageDao.getAllPending()

        assertThat(pending.map { it.messageTid }).containsExactly(3L, 2L, 1L).inOrder()
        assertThat(pending.map { it.syncState }).containsExactly(
            PinSyncStates.PENDING_PIN,
            PinSyncStates.PENDING_UNPIN,
            PinSyncStates.PENDING_PIN,
        ).inOrder()
    }

    @Test
    fun deletingTheMessageCascadesToThePin() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 2L, serverPinId = 2L))

        messageDao.deleteMessageByTid(1L)

        assertThat(pinnedMessageDao.countByChannel(CHANNEL_ID)).isEqualTo(1)
        assertThat(pinnedMessageDao.getByTid(1L, CHANNEL_ID)).isNull()
    }

    @Test
    fun aPendingUnpinIsHiddenFromDisplayButStillPending() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.markPendingUnpinWithMirror(1L, CHANNEL_ID, NOW)

        assertThat(pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)).isEmpty()
        // Still a durable intent, so the reconnect flush can retry it.
        assertThat(pinnedMessageDao.getAllPending().map { it.messageTid }).containsExactly(1L)
    }

    @Test
    fun aPendingUnpinDoesNotCountAsVisible() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.markPendingUnpinWithMirror(1L, CHANNEL_ID, NOW)

        assertThat(pinnedMessageDao.countByChannel(CHANNEL_ID)).isEqualTo(0)
    }

    @Test
    fun reconcileCandidatesNeverIncludePendingRows() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 2L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN
            )
        )

        // The server reported nothing at all.
        val stale = pinnedMessageDao.getSyncedExcluding(CHANNEL_ID, emptyList())

        // Only the synced pin is a deletion candidate; the intent the server has not been
        // told about survives.
        assertThat(stale.map { it.messageTid }).containsExactly(1L)
    }

    @Test
    fun upsertWritesTheMirrorAndDeleteClearsIt() = runTest {
        insertMessage(tid = 1L)

        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        assertThat(messageDao.getMessageByTid(1L)?.messageEntity?.pinDetails?.isPinned).isTrue()

        pinnedMessageDao.deleteWithMirror(1L, CHANNEL_ID)
        assertThat(messageDao.getMessageByTid(1L)?.messageEntity?.pinDetails).isNull()
    }

    @Test
    fun aPinForAMessageThatIsNotStoredIsSkippedRatherThanBreakingTheTransaction() = runTest {
        // The foreign key to the message is deferred, so this pin would not fail here — it
        // would bring the whole transaction down at commit, taking unrelated writes with it.
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 404L, serverPinId = 1L))

        assertThat(pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)).isEmpty()
    }

    @Test
    fun aPersonalPinWritesThePersonalScopeToTheMessageMirror() = runTest {
        insertMessage(tid = 1L)

        pinnedMessageDao.upsertWithMirror(
            pin(messageTid = 1L, serverPinId = 1L, pinScope = StoredPinScope.ForMe.value)
        )

        assertThat(messageDao.getMessageByTid(1L)?.messageEntity?.pinDetails?.pinType)
            .isEqualTo(com.sceyt.chat.models.message.PinDetails.PinType.PERSONAL)
    }

    @Test
    fun confirmingAPinReplacesTheSentinelAndResetsItsRetryState() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 1L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN,
                retryCount = 2,
            )
        )

        pinnedMessageDao.markSynced(1L, CHANNEL_ID, 500L)

        val stored = pinnedMessageDao.getByTid(1L, CHANNEL_ID)
        assertThat(stored?.serverPinId).isEqualTo(500L)
        assertThat(stored?.syncState).isEqualTo(PinSyncState.Synced.value)
        assertThat(stored?.retryCount).isEqualTo(0)
    }

    @Test
    fun aFailedAttemptKeepsItsIntentAndMovesItBehindOlderWork() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 1L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN,
                lastAttemptAt = 100L,
            )
        )
        pinnedMessageDao.upsertWithMirror(
            pin(
                messageTid = 2L,
                serverPinId = UNKNOWN_SERVER_PIN_ID,
                syncState = PinSyncStates.PENDING_PIN,
                lastAttemptAt = 200L,
            )
        )

        pinnedMessageDao.incrementRetry(1L, CHANNEL_ID, 300L)

        val pending = pinnedMessageDao.getAllPending()
        assertThat(pending.map { it.messageTid }).containsExactly(2L, 1L).inOrder()
        assertThat(pending.last().retryCount).isEqualTo(1)
        assertThat(pending.last().syncState).isEqualTo(PinSyncState.PendingPin.value)
    }

    @Test
    fun deletingAllPinsForAChannelClearsEveryMessageMirrorInThatChannel() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 2L, serverPinId = 2L))

        pinnedMessageDao.deleteAllByChannelWithMirrors(CHANNEL_ID)

        assertThat(pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)).isEmpty()
        assertThat(messageDao.getMessageByTid(1L)?.messageEntity?.pinDetails).isNull()
        assertThat(messageDao.getMessageByTid(2L)?.messageEntity?.pinDetails).isNull()
    }

    @Test
    fun clearingHistoryBeforeADateDeletesOnlyOlderPins() = runTest {
        insertMessage(tid = 1L, createdAt = 100L)
        insertMessage(tid = 2L, createdAt = 200L)
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 1L, serverPinId = 1L, messageCreatedAt = 100L))
        pinnedMessageDao.upsertWithMirror(pin(messageTid = 2L, serverPinId = 2L, messageCreatedAt = 200L))

        pinnedMessageDao.deleteAllByChannelBefore(CHANNEL_ID, 100L)

        assertThat(pinnedMessageDao.getByTid(1L, CHANNEL_ID)).isNull()
        assertThat(pinnedMessageDao.getByTid(2L, CHANNEL_ID)).isNotNull()
    }

    @Test
    fun anExpiredPinIsExcludedFromDisplay() = runTest {
        insertMessage(tid = 1L)
        pinnedMessageDao.upsertWithMirror(
            pin(messageTid = 1L, serverPinId = 1L, pinnedUntil = NOW - 1L)
        )

        assertThat(pinnedMessageDao.getPinnedMessages(CHANNEL_ID, NOW)).isEmpty()
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
                    pinDetails = null,
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
            )
        )
    }

    private fun pin(
        messageTid: Long,
        serverPinId: Long,
        syncState: Int = PinSyncState.Synced.value,
        pinnedUntil: Long? = null,
        pinScope: Int = StoredPinScope.ForAll.value,
        messageCreatedAt: Long = messageTid,
        retryCount: Int = 0,
        lastAttemptAt: Long = 0L,
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
        syncState = syncState,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt,
    )

    private companion object {
        const val CHANNEL_ID = 1L
        const val NOW = 10_000L
    }
}
