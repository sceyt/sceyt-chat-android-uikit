package com.sceyt.chatuikit.persistence.logicimpl.channel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocalUnreadCountsManagerTest {

    private lateinit var database: SceytDatabase
    private lateinit var manager: LocalUnreadCountsManager

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        manager = LocalUnreadCountsManager(
            localUnreadDao = database.localUnreadDao(),
            channelDao = database.channelDao(),
            channelsCache = ChannelsCache()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun baselineCountIsShownBeforeMessagesAreSynced() = runTest {
        val channel = channel(newMessageCount = 1000, lastMessageId = 1000)

        val seeded = manager.seedChannel(channel)

        assertThat(seeded.newMessageCount).isEqualTo(1000)
        assertThat(seeded.unread).isTrue()
    }

    @Test
    fun syncingMessagesInsideBaselineDoesNotDoubleCount() = runTest {
        val channel = manager.seedChannel(channel(newMessageCount = 1000, lastMessageId = 1000))

        manager.recordObservedMessages((1L..1000L).map { message(it) })
        val updated = manager.applyLocalState(channel)

        assertThat(updated.newMessageCount).isEqualTo(1000)
    }

    @Test
    fun newMessageAfterBaselineIncrementsOnce() = runTest {
        val channel = manager.seedChannel(channel(newMessageCount = 1000, lastMessageId = 1000))

        manager.recordObservedMessages(listOf(message(1001), message(1001)))
        val updated = manager.applyLocalState(channel)

        assertThat(updated.newMessageCount).isEqualTo(1001)
    }

    @Test
    fun localMarkReadClearsBaselineAndBlocksServerRefreshFromRestoringIt() = runTest {
        val channel = manager.seedChannel(channel(newMessageCount = 1000, lastMessageId = 1000))

        val read = manager.markRead(channel)
        val refreshed = manager.seedChannel(channel(newMessageCount = 1000, lastMessageId = 1000))

        assertThat(read.newMessageCount).isEqualTo(0)
        assertThat(read.unread).isFalse()
        assertThat(read.lastDisplayedMessageId).isEqualTo(1000)
        assertThat(refreshed.newMessageCount).isEqualTo(0)
        assertThat(refreshed.unread).isFalse()
        assertThat(refreshed.lastDisplayedMessageId).isEqualTo(1000)
    }

    private fun channel(
        id: Long = 1,
        newMessageCount: Long = 0,
        lastMessageId: Long = 0,
    ) = SceytChannel(
        id = id,
        parentChannelId = null,
        uri = null,
        type = "public",
        subject = "Channel",
        avatarUrl = null,
        metadata = null,
        createdAt = 0,
        updatedAt = 0,
        messagesClearedAt = 0,
        memberCount = 0,
        createdBy = null,
        userRole = "owner",
        unread = newMessageCount > 0,
        newMessageCount = newMessageCount,
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
        lastMessage = message(lastMessageId),
        messages = null,
        members = null,
        newReactions = null,
        pendingReactions = null,
        pending = false,
        draftMessage = null,
        events = null
    )

    private fun message(id: Long) = SceytMessage(
        id = id,
        tid = id,
        channelId = 1,
        body = "",
        type = "text",
        metadata = null,
        createdAt = id,
        updatedAt = id,
        incoming = true,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = MessageDeliveryStatus.Received,
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
