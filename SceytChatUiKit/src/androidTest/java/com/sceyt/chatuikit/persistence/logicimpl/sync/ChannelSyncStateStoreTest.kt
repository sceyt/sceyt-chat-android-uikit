package com.sceyt.chatuikit.persistence.logicimpl.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChannelSyncStateStoreTest {

    private lateinit var database: SceytDatabase
    private lateinit var store: ChannelSyncStateStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        store = ChannelSyncStateStore(database.channelSyncStateDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun isMessagesSynced_returnsFalse_whenNoSyncStateExists() = runTest {
        assertThat(store.isMessagesSynced(channelId = 1L, lastMessageId = 100L)).isFalse()
    }

    @Test
    fun isMessagesSynced_returnsTrue_whenLastMessageIdMatchesSavedState() = runTest {
        insertChannel(id = 1L)
        store.updateSyncState(channelId = 1L, lastSyncedMessageId = 100L)

        assertThat(store.isMessagesSynced(channelId = 1L, lastMessageId = 100L)).isTrue()
    }

    @Test
    fun isMessagesSynced_returnsFalse_whenLastMessageIdDoesNotMatchSavedState() = runTest {
        insertChannel(id = 1L)
        store.updateSyncState(channelId = 1L, lastSyncedMessageId = 100L)

        assertThat(store.isMessagesSynced(channelId = 1L, lastMessageId = 101L)).isFalse()
    }

    @Test
    fun updateSyncState_savesLastSyncedMessageId() = runTest {
        insertChannel(id = 1L)

        store.updateSyncState(channelId = 1L, lastSyncedMessageId = 100L)

        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 1L))
            .isEqualTo(100L)
    }

    @Test
    fun updateSyncStateForMessage_savesMessageIdAsLastSyncedMessageId() = runTest {
        insertChannel(id = 1L)

        store.updateSyncStateForMessage(channelId = 1L, messageId = 200L)

        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 1L))
            .isEqualTo(200L)
    }

    @Test
    fun deleteSyncState_removesStateForChannel() = runTest {
        insertChannel(id = 1L)
        store.updateSyncState(channelId = 1L, lastSyncedMessageId = 100L)

        store.deleteSyncState(channelId = 1L)

        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 1L))
            .isEqualTo(null)
    }

    @Test
    fun deleteSyncStates_removesStateForChannels() = runTest {
        insertChannel(id = 1L)
        insertChannel(id = 2L)
        insertChannel(id = 3L)
        store.updateSyncState(channelId = 1L, lastSyncedMessageId = 100L)
        store.updateSyncState(channelId = 2L, lastSyncedMessageId = 200L)
        store.updateSyncState(channelId = 3L, lastSyncedMessageId = 300L)

        store.deleteSyncStates(channelIds = listOf(1L, 2L))

        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 1L))
            .isEqualTo(null)
        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 2L))
            .isEqualTo(null)
        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(channelId = 3L))
            .isEqualTo(300L)
    }

    private suspend fun insertChannel(id: Long) {
        database.channelDao().insertChannelsAndLinks(listOf(channel(id)), emptyList())
    }

    private fun channel(id: Long) = ChannelEntity(
        id = id,
        parentChannelId = null,
        uri = null,
        type = "public",
        subject = "Channel $id",
        avatarUrl = null,
        metadata = null,
        createdAt = id,
        updatedAt = 0,
        messagesClearedAt = 0,
        memberCount = 0,
        createdById = null,
        userRole = "owner",
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
        lastMessageTid = null,
        lastMessageAt = null,
        pending = false,
        isSelf = false,
    )
}
