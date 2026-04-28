package com.sceyt.chatuikit.persistence.dao.channeldaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChannelDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var channelDao: ChannelDao

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
        channelDao = database.channelDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region Helpers

    private fun channel(
        id: Long,
        type: String = "public",
        subject: String? = "Channel $id",
        userRole: String? = "owner",
        pending: Boolean = false,
        lastMessageTid: Long? = null,
        lastMessageAt: Long? = null,
        createdAt: Long = id,
        pinnedAt: Long? = null,
        newMessageCount: Long = 0,
        uri: String? = null,
        isSelf: Boolean = false,
    ) = ChannelEntity(
        id = id,
        parentChannelId = null,
        uri = uri,
        type = type,
        subject = subject,
        avatarUrl = null,
        metadata = null,
        createdAt = createdAt,
        updatedAt = 0,
        messagesClearedAt = 0,
        memberCount = 0,
        createdById = null,
        userRole = userRole,
        unread = false,
        newMessageCount = newMessageCount,
        newMentionCount = 0,
        newReactedMessageCount = 0,
        hidden = false,
        archived = false,
        muted = false,
        mutedTill = null,
        pinnedAt = pinnedAt,
        lastReceivedMessageId = 0,
        lastDisplayedMessageId = 0,
        messageRetentionPeriod = 0,
        lastMessageTid = lastMessageTid,
        lastMessageAt = lastMessageAt,
        pending = pending,
        isSelf = isSelf,
    )

    private fun link(userId: String, chatId: Long, role: String = "member") =
        UserChatLinkEntity(userId = userId, chatId = chatId, role = role)

    private suspend fun insert(vararg channels: ChannelEntity) {
        channelDao.insertChannelsAndLinks(channels.toList(), emptyList())
    }

    // endregion

    // region getChannels

    @Test
    fun getChannels_returnsChannelsOrderedByPinnedFirst() = runTest {
        // Given
        insert(
            channel(1, createdAt = 100),
            channel(2, createdAt = 200, pinnedAt = 1000),
            channel(3, createdAt = 300, pinnedAt = 2000),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then — pinned channels come first ordered by pinnedAt DESC, then by createdAt DESC
        assertThat(result.map { it.channelEntity.id }).isEqualTo(listOf(3L, 2L, 1L))
    }

    @Test
    fun getChannels_ordersByLastMessageAtWhenEnabled() = runTest {
        // Given
        insert(
            channel(1, lastMessageAt = 300),
            channel(2, lastMessageAt = 100),
            channel(3, lastMessageAt = 200),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = true, onlyMine = false
        )

        // Then — ordered by lastMessageAt DESC
        assertThat(result.map { it.channelEntity.id }).isEqualTo(listOf(1L, 3L, 2L))
    }

    @Test
    fun getChannels_ordersByCreatedAtWhenLastMessageDisabled() = runTest {
        // Given
        insert(
            channel(1, createdAt = 100, lastMessageAt = 300),
            channel(2, createdAt = 300, lastMessageAt = 100),
            channel(3, createdAt = 200, lastMessageAt = 200),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then — ordered by createdAt DESC, lastMessageAt is ignored
        assertThat(result.map { it.channelEntity.id }).isEqualTo(listOf(2L, 3L, 1L))
    }

    @Test
    fun getChannels_onlyMine_excludesChannelsWithEmptyOrNullRole() = runTest {
        // Given
        insert(
            channel(1, userRole = "owner"),
            channel(2, userRole = ""),
            channel(3, userRole = null),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = true
        )

        // Then — only channels with a non-empty role are included
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun getChannels_onlyMineFalse_returnsAllRoles() = runTest {
        // Given
        insert(
            channel(1, userRole = "owner"),
            channel(2, userRole = ""),
            channel(3, userRole = null),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then — all channels returned regardless of role
        assertThat(result.map { it.channelEntity.id }).containsExactlyElementsIn(listOf(1L, 2L, 3L))
    }

    @Test
    fun getChannels_excludesPendingChannelWithNoMessages() = runTest {
        // Given — channel 2 is pending and has no message (lastMessageTid = 0/null)
        insert(
            channel(1),
            channel(2, pending = true, lastMessageTid = 0),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun getChannels_includesPendingChannelThatHasMessage() = runTest {
        // Given — channel 2 is pending but has a sent message (lastMessageTid != 0)
        insert(
            channel(1),
            channel(2, pending = true, lastMessageTid = 42),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then — pending channel with a real message is included
        assertThat(result.map { it.channelEntity.id }).containsExactlyElementsIn(listOf(1L, 2L))
    }

    @Test
    fun getChannels_filtersByType() = runTest {
        // Given
        insert(
            channel(1, type = "public"),
            channel(2, type = "group"),
            channel(3, type = "direct"),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = listOf("public", "group"),
            orderByLastMessage = false, onlyMine = false
        )

        // Then
        assertThat(result.map { it.channelEntity.id }).containsExactlyElementsIn(listOf(1L, 2L))
    }

    @Test
    fun getChannels_emptyTypes_returnsAllTypes() = runTest {
        // Given
        insert(
            channel(1, type = "public"),
            channel(2, type = "group"),
            channel(3, type = "direct"),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then — empty types means no filter
        assertThat(result).hasSize(3)
    }

    @Test
    fun getChannels_respectsLimitAndOffset() = runTest {
        // Given — channels ordered by createdAt DESC: 4, 3, 2, 1
        insert(
            channel(1, createdAt = 100),
            channel(2, createdAt = 200),
            channel(3, createdAt = 300),
            channel(4, createdAt = 400),
        )

        // When
        val firstPage = channelDao.getChannels(
            limit = 2, offset = 0, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )
        val secondPage = channelDao.getChannels(
            limit = 2, offset = 2, types = emptyList(),
            orderByLastMessage = false, onlyMine = false
        )

        // Then
        assertThat(firstPage.map { it.channelEntity.id }).isEqualTo(listOf(4L, 3L))
        assertThat(secondPage.map { it.channelEntity.id }).isEqualTo(listOf(2L, 1L))
    }

    // region getChannelById / getChannelsById

    @Test
    fun getChannelById_returnsCorrectChannel() = runTest {
        insert(channel(1), channel(2))
        val result = channelDao.getChannelById(1L)
        assertThat(result?.channelEntity?.id).isEqualTo(1L)
    }

    @Test
    fun getChannelById_returnsNullForMissingId() = runTest {
        val result = channelDao.getChannelById(999L)
        assertThat(result).isNull()
    }

    @Test
    fun getChannelsById_returnsOnlyMatchingChannels() = runTest {
        insert(channel(1), channel(2), channel(3))
        val result = channelDao.getChannelsById(listOf(1L, 3L))
        assertThat(result.map { it.channelEntity.id }).containsExactlyElementsIn(listOf(1L, 3L))
    }

    // endregion

    // region getChannelByUserAndType

    @Test
    fun getChannelByUserAndType_returnsMatchingChannel() = runTest {
        // Given
        insert(channel(1, type = "direct"), channel(2, type = "direct"))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 2)))

        // When
        val result = channelDao.getChannelByUserAndType("alice", "direct")

        // Then
        assertThat(result?.channelEntity?.id).isEqualTo(1L)
    }

    @Test
    fun getChannelByUserAndType_returnsNullWhenNotFound() = runTest {
        val result = channelDao.getChannelByUserAndType("nobody", "direct")
        assertThat(result).isNull()
    }

    @Test
    fun getChannelByUserAndType_doesNotMatchWrongType() = runTest {
        // Given — channel is a group, not direct
        insert(channel(1, type = "group"))
        channelDao.insertUserChatLinks(listOf(link("alice", 1)))

        // When
        val result = channelDao.getChannelByUserAndType("alice", "direct")

        // Then
        assertThat(result).isNull()
    }

    // endregion

    // region getChannelByUsersAndType

    @Test
    fun getChannelByUsersAndType_returnsChannelMatchingAllUsers() = runTest {
        // Given — channel 1 has alice and bob; channel 2 has alice only
        insert(channel(1, type = "direct"), channel(2, type = "direct"))
        channelDao.insertUserChatLinks(listOf(
            link("alice", 1), link("bob", 1),
            link("alice", 2),
        ))

        // When — query for alice and bob (userCount = 2)
        val result = channelDao.getChannelByUsersAndType(listOf("alice", "bob"), "direct")

        // Then — channel 1 has exactly 2 matching users; channel 2 only has 1
        assertThat(result?.channelEntity?.id).isEqualTo(1L)
    }

    @Test
    fun getChannelByUsersAndType_returnsNullWhenNotEnoughUsersMatch() = runTest {
        // Given — channel has alice and bob, but we query for alice, bob, carol
        insert(channel(1, type = "direct"))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 1)))

        // When — carol is not a member, so only 2 of 3 queried users match
        val result = channelDao.getChannelByUsersAndType(listOf("alice", "bob", "carol"), "direct")

        // Then — COUNT(matched) = 2, userCount = 3 → no match
        assertThat(result).isNull()
    }

    // endregion

    // region getChannelByUri / getSelfChannel

    @Test
    fun getChannelByUri_returnsCorrectChannel() = runTest {
        insert(channel(1, uri = "my-channel"), channel(2, uri = "other"))
        val result = channelDao.getChannelByUri("my-channel")
        assertThat(result?.channelEntity?.id).isEqualTo(1L)
    }

    @Test
    fun getChannelByUri_returnsNullForMissingUri() = runTest {
        val result = channelDao.getChannelByUri("nonexistent")
        assertThat(result).isNull()
    }

    @Test
    fun getSelfChannel_returnsSelfChannel() = runTest {
        insert(channel(1, isSelf = false), channel(2, isSelf = true))
        val result = channelDao.getSelfChannel()
        assertThat(result?.channelEntity?.id).isEqualTo(2L)
    }

    @Test
    fun getSelfChannel_returnsNullWhenNone() = runTest {
        insert(channel(1, isSelf = false))
        val result = channelDao.getSelfChannel()
        assertThat(result).isNull()
    }

    // endregion

    // region getNotExistingChannelIdsByIdsAndTypes

    @Test
    fun getNotExistingChannelIdsByIdsAndTypes_returnsIdsNotInProvidedList() = runTest {
        // Given — DB has channels 1, 2, 3
        insert(channel(1), channel(2), channel(3))

        // When — we provide [1, 3] as "known" ids
        val result = channelDao.getNotExistingChannelIdsByIdsAndTypes(
            ids = listOf(1L, 3L), types = emptyList(), onlyMine = false
        )

        // Then — channel 2 exists in DB but not in provided list
        assertThat(result).containsExactly(2L)
    }

    @Test
    fun getNotExistingChannelIdsByIdsAndTypes_onlyMine_excludesNonMemberChannels() = runTest {
        // Given — channels 2 and 3 are not in provided ids; channel 3 has no role
        insert(
            channel(1, userRole = "owner"),
            channel(2, userRole = "owner"),
            channel(3, userRole = ""),
        )

        // When
        val result = channelDao.getNotExistingChannelIdsByIdsAndTypes(
            ids = listOf(1L), types = emptyList(), onlyMine = true
        )

        // Then — channel 3 excluded due to empty role; only channel 2 is returned
        assertThat(result).containsExactly(2L)
    }

    @Test
    fun getNotExistingChannelIdsByIdsAndTypes_filtersByType() = runTest {
        // Given
        insert(
            channel(1, type = "public"),
            channel(2, type = "group"),
            channel(3, type = "direct"),
        )

        // When — only look within public and group types
        val result = channelDao.getNotExistingChannelIdsByIdsAndTypes(
            ids = listOf(1L), types = listOf("public", "group"), onlyMine = false
        )

        // Then — channel 2 (group, not in provided ids); channel 3 excluded by type filter
        assertThat(result).containsExactly(2L)
    }

    @Test
    fun getNotExistingChannelIdsByIdsAndTypes_excludesPendingChannels() = runTest {
        // Given — channel 2 is pending with no message
        insert(
            channel(1),
            channel(2, pending = true),
        )

        // When — provide no ids so everything in DB would otherwise match
        val result = channelDao.getNotExistingChannelIdsByIdsAndTypes(
            ids = emptyList(), types = emptyList(), onlyMine = false
        )

        // Then — pending channel is excluded
        assertThat(result).containsExactly(1L)
    }

    // endregion

    // region getAllChannelIdsByTypes

    @Test
    fun getAllChannelIdsByTypes_returnsAllNonPendingIds() = runTest {
        // Given
        insert(channel(1), channel(2), channel(3, pending = true))

        // When
        val result = channelDao.getAllChannelIdsByTypes(types = emptyList(), onlyMine = false)

        // Then — pending channel excluded
        assertThat(result).containsExactlyElementsIn(listOf(1L, 2L))
    }

    @Test
    fun getAllChannelIdsByTypes_filtersByType() = runTest {
        // Given
        insert(
            channel(1, type = "public"),
            channel(2, type = "group"),
            channel(3, type = "direct"),
        )

        // When
        val result = channelDao.getAllChannelIdsByTypes(
            types = listOf("public", "direct"), onlyMine = false
        )

        // Then
        assertThat(result).containsExactlyElementsIn(listOf(1L, 3L))
    }

    @Test
    fun getAllChannelIdsByTypes_onlyMine_excludesNonMemberChannels() = runTest {
        // Given
        insert(
            channel(1, userRole = "owner"),
            channel(2, userRole = ""),
            channel(3, userRole = null),
        )

        // When
        val result = channelDao.getAllChannelIdsByTypes(types = emptyList(), onlyMine = true)

        // Then
        assertThat(result).containsExactly(1L)
    }

    @Test
    fun getChannels_pinnedWinsOverHigherLastMessageAtWhenOrderByLastMessageEnabled() = runTest {
        // Given — channel 2 is unpinned but has a more recent lastMessageAt than pinned channel 1
        insert(
            channel(1, lastMessageAt = 100, pinnedAt = 1000),
            channel(2, lastMessageAt = 999),
        )

        // When
        val result = channelDao.getChannels(
            limit = 10, offset = 0, types = emptyList(),
            orderByLastMessage = true, onlyMine = false
        )

        // Then — pinned channel 1 still appears first despite lower lastMessageAt
        assertThat(result.map { it.channelEntity.id }).isEqualTo(listOf(1L, 2L))
    }

    // endregion

    // region getTotalUnreadCountAsFlow

    @Test
    fun getTotalUnreadCount_returnsSumOfAllChannelUnreadCounts() = runTest {
        // Given
        insert(channel(1, newMessageCount = 3), channel(2, newMessageCount = 7))

        // When
        val result = channelDao.getTotalUnreadCountAsFlow(channelTypes = emptyList()).first()

        // Then
        assertThat(result).isEqualTo(10L)
    }

    @Test
    fun getTotalUnreadCount_filtersByChannelType() = runTest {
        // Given
        insert(
            channel(1, type = "public", newMessageCount = 3),
            channel(2, type = "group", newMessageCount = 7),
        )

        // When — only count unread in public channels
        val result = channelDao.getTotalUnreadCountAsFlow(channelTypes = listOf("public")).first()

        // Then
        assertThat(result).isEqualTo(3L)
    }

    @Test
    fun getTotalUnreadCount_returnsNullWhenNoChannels() = runTest {
        // When — empty database
        val result = channelDao.getTotalUnreadCountAsFlow(channelTypes = emptyList()).first()

        // Then — SUM of empty set is NULL in SQLite
        assertThat(result).isNull()
    }

    @Test
    fun getTotalUnreadCount_emitsUpdatedValueAfterInsert() = runTest {
        val flow = channelDao.getTotalUnreadCountAsFlow(channelTypes = emptyList())

        // Initial state — empty DB
        assertThat(flow.first()).isNull()

        // After insert the flow reflects new state
        insert(channel(1, newMessageCount = 5))
        assertThat(flow.first()).isEqualTo(5L)
    }

    // endregion

    // region update queries

    @Test
    fun updateLastMessage_updatesLastMessageTidAndAt() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updateLastMessage(channelId = 1L, lastMessageTid = 99L, lastMessageAt = 12345L)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.lastMessageTid).isEqualTo(99L)
        assertThat(updated.lastMessageAt).isEqualTo(12345L)
    }

    @Test
    fun updateUnreadCount_updatesNewMessageCount() = runTest {
        // Given
        insert(channel(1, newMessageCount = 5))

        // When
        channelDao.updateUnreadCount(channelId = 1L, count = 0)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.newMessageCount).isEqualTo(0L)
    }

    @Test
    fun updateMemberCount_updatesCount() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updateMemberCount(channelId = 1L, count = 42)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.memberCount).isEqualTo(42L)
    }

    @Test
    fun updateMuteState_updatesMutedAndMutedTill() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updateMuteState(channelId = 1L, muted = true, muteUntil = 9999L)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.muted).isTrue()
        assertThat(updated.mutedTill).isEqualTo(9999L)
    }

    @Test
    fun updatePinState_updatesPinnedAt() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updatePinState(channelId = 1L, pinnedAt = 5000L)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.pinnedAt).isEqualTo(5000L)
    }

    @Test
    fun updatePinState_clearsPin() = runTest {
        // Given — channel is pinned
        insert(channel(1, pinnedAt = 5000L))

        // When
        channelDao.updatePinState(channelId = 1L, pinnedAt = null)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.pinnedAt).isNull()
    }

    // endregion

    // region delete queries

    @Test
    fun deleteChannel_removesChannel() = runTest {
        // Given
        insert(channel(1), channel(2))

        // When
        channelDao.deleteChannel(1L)

        // Then
        assertThat(channelDao.getChannelById(1L)).isNull()
        assertThat(channelDao.getChannelById(2L)).isNotNull()
    }

    @Test
    fun deleteChannelAndLinks_removesChannelAndAllItsLinks() = runTest {
        // Given
        insert(channel(1))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 1)))

        // When
        channelDao.deleteChannelAndLinks(1L)

        // Then
        assertThat(channelDao.getChannelById(1L)).isNull()
        assertThat(channelDao.getUserChannelLinksByPeerId("alice")).isEmpty()
        assertThat(channelDao.getUserChannelLinksByPeerId("bob")).isEmpty()
    }

    @Test
    fun deleteUserChatLinks_removesOnlySpecifiedUserLink() = runTest {
        // Given
        insert(channel(1))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 1)))

        // When — remove only alice's link
        channelDao.deleteUserChatLinks(1L, "alice")

        // Then
        assertThat(channelDao.getUserChannelLinksByPeerId("alice")).isEmpty()
        assertThat(channelDao.getUserChannelLinksByPeerId("bob")).hasSize(1)
    }

    @Test
    fun deleteAllChannelsAndLinksById_removesAllSpecifiedChannels() = runTest {
        // Given
        insert(channel(1), channel(2), channel(3))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 2)))

        // When
        channelDao.deleteAllChannelsAndLinksById(listOf(1L, 2L))

        // Then
        assertThat(channelDao.getChannelById(1L)).isNull()
        assertThat(channelDao.getChannelById(2L)).isNull()
        assertThat(channelDao.getChannelById(3L)).isNotNull()
        assertThat(channelDao.getUserChannelLinksByPeerId("alice")).isEmpty()
    }

    @Test
    fun deleteChatLinksExceptUser_keepsOnlySpecifiedUser() = runTest {
        // Given
        insert(channel(1))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 1), link("carol", 1)))

        // When
        channelDao.deleteChatLinksExceptUser(channelId = 1L, exceptUserId = "alice")

        // Then — alice's link survives, others are removed
        assertThat(channelDao.getUserChannelLinksByPeerId("alice")).hasSize(1)
        assertThat(channelDao.getUserChannelLinksByPeerId("bob")).isEmpty()
        assertThat(channelDao.getUserChannelLinksByPeerId("carol")).isEmpty()
    }

    // endregion

    // region scalar queries

    @Test
    fun getAllChannelsIds_returnsAllIds() = runTest {
        // Given — includes a pending channel since this query has no filter
        insert(channel(1), channel(2), channel(3, pending = true))

        // When
        val result = channelDao.getAllChannelsIds()

        // Then — all channels returned, including pending
        assertThat(result).containsExactlyElementsIn(listOf(1L, 2L, 3L))
    }

    @Test
    fun getAllChannelsCount_returnsCorrectCount() = runTest {
        insert(channel(1), channel(2), channel(3))
        assertThat(channelDao.getAllChannelsCount()).isEqualTo(3)
    }

    @Test
    fun getChannelLastMessageTid_returnsCorrectTid() = runTest {
        insert(channel(1, lastMessageTid = 77L))
        assertThat(channelDao.getChannelLastMessageTid(1L)).isEqualTo(77L)
    }

    @Test
    fun getChannelLastMessageTid_returnsNullForMissingChannel() = runTest {
        assertThat(channelDao.getChannelLastMessageTid(999L)).isNull()
    }

    @Test
    fun getChannelsLastMessageTIds_returnsOnlyTidsForMatchingIds() = runTest {
        insert(
            channel(1, lastMessageTid = 10L),
            channel(2, lastMessageTid = 20L),
            channel(3, lastMessageTid = 30L),
        )

        val result = channelDao.getChannelsLastMessageTIds(listOf(1L, 3L))

        assertThat(result).containsExactlyElementsIn(listOf(10L, 30L))
    }

    @Test
    fun getRetentionPeriodByChannelId_returnsCorrectPeriod() = runTest {
        // ChannelEntity.messageRetentionPeriod defaults to 0 in the helper; override via full entity
        val entity = channel(1).copy(messageRetentionPeriod = 86400L)
        channelDao.insertChannelsAndLinks(listOf(entity), emptyList())

        assertThat(channelDao.getRetentionPeriodByChannelId(1L)).isEqualTo(86400L)
    }

    // endregion

    // region remaining update queries

    @Test
    fun updateLastMessageWithLastRead_updatesAllThreeFields() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updateLastMessageWithLastRead(
            channelId = 1L,
            lastMessageTid = 55L,
            lastMessageId = 42L,
            lastMessageAt = 99999L
        )

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.lastMessageTid).isEqualTo(55L)
        assertThat(updated.lastMessageAt).isEqualTo(99999L)
        assertThat(updated.lastDisplayedMessageId).isEqualTo(42L)
    }

    @Test
    fun updateAutoDeleteState_updatesRetentionPeriod() = runTest {
        // Given
        insert(channel(1))

        // When
        channelDao.updateAutoDeleteState(channelId = 1L, period = 3600L)

        // Then
        val updated = channelDao.getChannelById(1L)!!.channelEntity
        assertThat(updated.messageRetentionPeriod).isEqualTo(3600L)
    }

    // endregion
}
