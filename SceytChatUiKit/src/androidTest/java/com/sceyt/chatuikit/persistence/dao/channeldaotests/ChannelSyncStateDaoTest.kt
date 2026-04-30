package com.sceyt.chatuikit.persistence.dao.channeldaotests

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.ChannelSyncStateDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelSyncStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChannelSyncStateDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var dao: ChannelSyncStateDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        dao = database.channelSyncStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region Helpers

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

    private suspend fun insertChannel(id: Long) {
        database.channelDao().insertChannelsAndLinks(listOf(channel(id)), emptyList())
    }

    private fun syncState(channelId: Long, lastSyncedMessageId: Long) =
        ChannelSyncStateEntity(channelId = channelId, lastSyncedMessageId = lastSyncedMessageId)

    // endregion

    @Test
    fun getLastSyncedMessageId_returnsZero_whenNoRowExists() = runTest {
        val result = dao.getLastSyncedMessageId(channelId = 1L)
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun upsertChannelSyncState_insertsRow_whenChannelExists() = runTest {
        insertChannel(id = 1L)
        dao.upsertChannelSyncState(syncState(channelId = 1L, lastSyncedMessageId = 100L))

        val result = dao.getLastSyncedMessageId(channelId = 1L)
        assertThat(result).isEqualTo(100L)
    }

    @Test
    fun upsertChannelSyncState_skipsInsert_whenChannelDoesNotExist() = runTest {
        dao.upsertChannelSyncState(syncState(channelId = 99L, lastSyncedMessageId = 100L))

        val result = dao.getLastSyncedMessageId(channelId = 99L)
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun upsertChannelSyncState_updatesExistingRow() = runTest {
        insertChannel(id = 1L)
        dao.upsertChannelSyncState(syncState(channelId = 1L, lastSyncedMessageId = 100L))
        dao.upsertChannelSyncState(syncState(channelId = 1L, lastSyncedMessageId = 200L))

        val result = dao.getLastSyncedMessageId(channelId = 1L)
        assertThat(result).isEqualTo(200L)
    }

    @Test
    fun deleteChannelSyncState_removesRow() = runTest {
        insertChannel(id = 1L)
        dao.upsertChannelSyncState(syncState(channelId = 1L, lastSyncedMessageId = 100L))

        dao.deleteChannelSyncState(channelId = 1L)

        val result = dao.getLastSyncedMessageId(channelId = 1L)
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun deleteChannelSyncStates_removesMultipleRows() = runTest {
        insertChannel(id = 1L)
        insertChannel(id = 2L)
        insertChannel(id = 3L)
        dao.upsertChannelSyncState(syncState(channelId = 1L, lastSyncedMessageId = 100L))
        dao.upsertChannelSyncState(syncState(channelId = 2L, lastSyncedMessageId = 200L))
        dao.upsertChannelSyncState(syncState(channelId = 3L, lastSyncedMessageId = 300L))

        dao.deleteChannelSyncStates(channelIds = listOf(1L, 2L))

        assertThat(dao.getLastSyncedMessageId(channelId = 1L)).isEqualTo(null)
        assertThat(dao.getLastSyncedMessageId(channelId = 2L)).isEqualTo(null)
        assertThat(dao.getLastSyncedMessageId(channelId = 3L)).isEqualTo(300L)
    }
}
