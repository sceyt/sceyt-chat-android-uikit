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
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PendingPinDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var pendingPinDao: PendingPinDao

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
        pendingPinDao = database.pendingPinDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aNewRequestReplacesThePreviousOneWithANewId() = runTest {
        insertMessage(tid = 1L)

        val pin = pendingPinDao.replace(request(messageTid = 1L, isPin = true))!!
        val unpin = pendingPinDao.replace(request(messageTid = 1L, isPin = false))!!

        assertThat(unpin.id).isNotEqualTo(pin.id)
        assertThat(pendingPinDao.getAll()).containsExactly(unpin)
    }

    @Test
    fun deleteByIdReportsWhetherTheRequestWasStillStored() = runTest {
        insertMessage(tid = 1L)
        val sent = pendingPinDao.replace(request(messageTid = 1L))!!
        // The user tapped again while the first request was in flight.
        val newer = pendingPinDao.replace(request(messageTid = 1L, isPin = false))!!

        assertThat(pendingPinDao.deleteById(sent.id)).isEqualTo(0)
        assertThat(pendingPinDao.getAll()).containsExactly(newer)

        assertThat(pendingPinDao.deleteById(newer.id)).isEqualTo(1)
        assertThat(pendingPinDao.getAll()).isEmpty()
    }

    @Test
    fun aRequestForAMessageThatIsNotStoredIsSkipped() = runTest {
        // The foreign key is deferred, so writing it would fail the whole transaction at commit.
        assertThat(pendingPinDao.replace(request(messageTid = 404L))).isNull()
        assertThat(pendingPinDao.getAll()).isEmpty()
    }

    @Test
    fun deleteByTidsRemovesOnlyThoseRequests() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L)
        pendingPinDao.replace(request(messageTid = 1L))
        val kept = pendingPinDao.replace(request(messageTid = 2L))

        pendingPinDao.deleteByTids(listOf(1L))

        assertThat(pendingPinDao.getAll()).containsExactly(kept)
    }

    @Test
    fun deletingTheMessageCascadesToItsRequest() = runTest {
        insertMessage(tid = 1L)
        pendingPinDao.replace(request(messageTid = 1L))

        messageDao.deleteMessageByTid(1L)

        assertThat(pendingPinDao.getByTid(1L)).isNull()
    }

    @Test
    fun theMessageCarriesItsPendingRequest() = runTest {
        insertMessage(tid = 1L)
        val stored = pendingPinDao.replace(request(messageTid = 1L, isPin = false))

        assertThat(messageDao.getMessageByTid(1L)?.pendingPin).isEqualTo(stored)
    }

    @Test
    fun pendingPinsAreScopedToTheirChannel() = runTest {
        insertMessage(tid = 1L)
        insertMessage(tid = 2L, channelId = OTHER_CHANNEL_ID)
        pendingPinDao.replace(request(messageTid = 1L))
        pendingPinDao.replace(request(messageTid = 2L, channelId = OTHER_CHANNEL_ID))

        assertThat(pendingPinDao.getByChannel(CHANNEL_ID).map { it.messageTid }).containsExactly(1L)
        assertThat(pendingPinDao.getPendingPinsFlow(CHANNEL_ID).first().map { it.message?.messageEntity?.tid })
            .containsExactly(1L)
    }

    private fun request(
        messageTid: Long,
        isPin: Boolean = true,
        channelId: Long = CHANNEL_ID,
    ) = PendingPinEntity(
        messageTid = messageTid,
        channelId = channelId,
        messageId = messageTid,
        isPin = isPin,
        pinScope = 2,
        createdAt = 0L,
    )

    private suspend fun insertMessage(tid: Long, channelId: Long = CHANNEL_ID) {
        messageDao.upsertMessage(
            MessageDb(
                messageEntity = MessageEntity(
                    tid = tid,
                    id = tid,
                    channelId = channelId,
                    body = "body",
                    type = "text",
                    metadata = null,
                    createdAt = tid,
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

    private companion object {
        const val CHANNEL_ID = 1L
        const val OTHER_CHANNEL_ID = 2L
    }
}
