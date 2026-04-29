package com.sceyt.chatuikit.persistence.dao.searchdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.LinkDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
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
class SearchChannelDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var userDao: UserDao
    private lateinit var channelDao: ChannelDao
    private lateinit var messageDao: MessageDao
    private lateinit var globalSearchDao: GlobalSearchDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var linkDao: LinkDao

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
        userDao = database.userDao()
        channelDao = database.channelDao()
        messageDao = database.messageDao()
        globalSearchDao = database.globalSearchDao()
        attachmentDao = database.attachmentsDao()
        linkDao = database.linkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchUsersLinkedToJoinedChannelsByDisplayName_returnsDistinctJoinedMembersAndExcludesCurrentUser() =
        runTest {
            insertUsers(
                user("me", firstName = "Jamie", lastName = "Stone"),
                user("jade", firstName = "Jade", lastName = "Morgan"),
                user("john", firstName = "John", lastName = "Carter"),
                user("jane", firstName = "Jane", lastName = "Miles"),
            )
            insertChannelsAndLinks(
                channels = listOf(
                    channel(id = 1, subject = "Joined", userRole = "owner"),
                    channel(id = 2, subject = "Not Joined", userRole = ""),
                    channel(id = 3, subject = "Also Joined", userRole = "member"),
                ),
                links = listOf(
                    link("me", 1),
                    link("jade", 1),
                    link("john", 1),
                    link("jade", 3),
                    link("jane", 2),
                )
            )

            val result = userDao.searchUsersLinkedToJoinedChannelsByDisplayName(
                searchQuery = "Ja",
                excludedUserId = "me",
                limit = 10,
            )

            assertThat(result.map { it.id }).containsExactly("jade")
        }

    @Test
    fun searchUsersLinkedToJoinedChannelsByDisplayName_prioritizesFullNamePrefixBeforeUsernamePrefix() =
        runTest {
            insertUsers(
                user("jade", firstName = "Jade", lastName = "Morgan", username = "jade"),
                user("zoe", firstName = "Zoe", lastName = "Lane", username = "ja_zoe"),
            )
            insertChannelsAndLinks(
                channels = listOf(channel(id = 10, subject = "Joined", userRole = "owner")),
                links = listOf(link("jade", 10), link("zoe", 10)),
            )

            val result = userDao.searchUsersLinkedToJoinedChannelsByDisplayName(
                searchQuery = "Ja",
                excludedUserId = null,
                limit = 10,
            )

            assertThat(result.map { it.id }).containsExactly("jade", "zoe").inOrder()
        }

    @Test
    fun searchChannelsBySubjectAndTypes_blankQueryReturnsAllNonPendingMatchingTypeChannels() =
        runTest {
            insertChannelsAndLinks(
                channels = listOf(
                    channel(
                        id = 1,
                        type = ChannelTypeEnum.Public.value,
                        subject = "Engineering",
                        lastMessageTid = 1,
                        lastMessageAt = 100
                    ),
                    channel(
                        id = 2,
                        type = ChannelTypeEnum.Public.value,
                        subject = "Design",
                        lastMessageTid = 2,
                        lastMessageAt = 200
                    ),
                    channel(
                        id = 3,
                        type = ChannelTypeEnum.Public.value,
                        subject = "Pending",
                        lastMessageTid = 0,
                        pending = true,
                        lastMessageAt = null
                    ),
                ),
                links = emptyList(),
            )

            val result = globalSearchDao.searchChannelsBySubjectAndTypes(
                query = "",
                types = listOf(ChannelTypeEnum.Public.value),
                limit = 10,
                offset = 0,
            )

            assertThat(result.map { it.channelEntity.id }).containsExactly(2L, 1L).inOrder()
        }

    @Test
    fun searchChannelsBySubjectAndTypes_filtersBySubjectContains() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Engineering Team",
                    lastMessageTid = 1,
                    lastMessageAt = 100
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Design Team",
                    lastMessageTid = 2,
                    lastMessageAt = 200
                ),
                channel(
                    id = 3,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Marketing",
                    lastMessageTid = 3,
                    lastMessageAt = 300
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "Team",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchChannelsBySubjectAndTypes_symbolSeparatedSubjectMatchesLaterToken() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 4,
                    type = ChannelTypeEnum.Public.value,
                    subject = "public-channel",
                    lastMessageTid = 4,
                    lastMessageAt = 400,
                ),
                channel(
                    id = 5,
                    type = ChannelTypeEnum.Public.value,
                    subject = "publicchannel",
                    lastMessageTid = 5,
                    lastMessageAt = 500,
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "channel",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(4L)
    }

    @Test
    fun searchChannelsBySubjectAndTypes_queryPunctuationSplitsIntoTokens() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 6,
                    type = ChannelTypeEnum.Public.value,
                    subject = "public channel",
                    lastMessageTid = 6,
                    lastMessageAt = 600,
                ),
                channel(
                    id = 7,
                    type = ChannelTypeEnum.Public.value,
                    subject = "public-channel",
                    lastMessageTid = 7,
                    lastMessageAt = 700,
                ),
                channel(
                    id = 8,
                    type = ChannelTypeEnum.Public.value,
                    subject = "public update",
                    lastMessageTid = 8,
                    lastMessageAt = 800,
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "public-channel",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(7L, 6L).inOrder()
    }


    @Test
    fun searchChannelsByUserIds_matchesGroupChannelBySubject() = runTest {
        // Given
        insert(
            channel(1, type = "public", subject = "Hello World"),
            channel(2, type = "public", subject = "Other Channel"),
        )

        // When
        val result = globalSearchDao.searchChannelsByUserIds(
            query = "Hello", userIds = emptyList(), limit = 10, offset = 0,
            onlyMine = false, types = emptyList(), orderByLastMessage = false,
            directType = "direct"
        )

        // Then
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsByUserIds_symbolSeparatedSubjectMatchesLaterToken() = runTest {
        insert(
            channel(1, type = "public", subject = "public-channel"),
            channel(2, type = "public", subject = "publicchannel"),
        )

        val result = globalSearchDao.searchChannelsByUserIds(
            query = "channel", userIds = emptyList(), limit = 10, offset = 0,
            onlyMine = false, types = emptyList(), orderByLastMessage = false,
            directType = "direct"
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsByUserIds_queryPunctuationSplitsIntoTokens() = runTest {
        insert(
            channel(1, type = "public", subject = "public channel"),
            channel(2, type = "public", subject = "public-channel"),
            channel(3, type = "public", subject = "public update"),
        )

        val result = globalSearchDao.searchChannelsByUserIds(
            query = "public-channel", userIds = emptyList(), limit = 10, offset = 0,
            onlyMine = false, types = emptyList(), orderByLastMessage = false,
            directType = "direct"
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchChannelsByUserIds_matchesDirectChannelByUserId() = runTest {
        // Given
        insert(
            channel(1, type = "direct", subject = ""),
            channel(2, type = "direct", subject = ""),
        )
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 2)))

        // When — search for channels where alice is a member
        val result = globalSearchDao.searchChannelsByUserIds(
            query = "", userIds = listOf("alice"), limit = 10, offset = 0,
            onlyMine = false, types = listOf("direct"), orderByLastMessage = false,
            directType = "direct"
        )

        // Then
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsByUserIds_onlyMine_excludesNonMemberChannels() = runTest {
        // Given
        insert(
            channel(1, type = "public", subject = "Alpha", userRole = "owner"),
            channel(2, type = "public", subject = "Alpha", userRole = ""),
        )

        // When
        val result = globalSearchDao.searchChannelsByUserIds(
            query = "Alpha", userIds = emptyList(), limit = 10, offset = 0,
            onlyMine = true, types = emptyList(), orderByLastMessage = false,
            directType = "direct"
        )

        // Then — channel 2 excluded because empty role
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsByUserIds_filtersByType() = runTest {
        // Given
        insert(
            channel(1, type = "public", subject = "Alpha"),
            channel(2, type = "group", subject = "Alpha"),
        )

        // When
        val result = globalSearchDao.searchChannelsByUserIds(
            query = "Alpha", userIds = emptyList(), limit = 10, offset = 0,
            onlyMine = false, types = listOf("public"), orderByLastMessage = false,
            directType = "direct"
        )

        // Then
        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsByUserIds_returnsChannelOnceWhenMultipleUsersMatch() = runTest {
        // Given — one channel with two members, both matching the search
        insert(channel(1, type = "direct", subject = ""))
        channelDao.insertUserChatLinks(listOf(link("alice", 1), link("bob", 1)))

        // When — both alice and bob are in userIds
        val result = globalSearchDao.searchChannelsByUserIds(
            query = "", userIds = listOf("alice", "bob"), limit = 10, offset = 0,
            onlyMine = false, types = listOf("direct"), orderByLastMessage = false,
            directType = "direct"
        )

        // Then — channel appears only once even though multiple links matched
        assertThat(result).hasSize(1)
    }


    @Test
    fun searchChannelsBySubjectAndTypes_filtersByType() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Public Alpha",
                    lastMessageTid = 1,
                    lastMessageAt = 100
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Group.value,
                    subject = "Group Beta",
                    lastMessageTid = 2,
                    lastMessageAt = 200
                ),
                channel(
                    id = 3,
                    type = ChannelTypeEnum.Direct.value,
                    subject = "Direct Gamma",
                    lastMessageTid = 3,
                    lastMessageAt = 300
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchChannelsBySubjectAndTypes_multipleTypesMatchAll() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Public Alpha",
                    lastMessageTid = 1,
                    lastMessageAt = 100
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Group.value,
                    subject = "Group Beta",
                    lastMessageTid = 2,
                    lastMessageAt = 200
                ),
                channel(
                    id = 3,
                    type = ChannelTypeEnum.Direct.value,
                    subject = "Direct Gamma",
                    lastMessageTid = 3,
                    lastMessageAt = 300
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "",
            types = listOf(ChannelTypeEnum.Public.value, ChannelTypeEnum.Group.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchChannelsBySubjectAndTypes_emptyTypesReturnsAllTypes() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Alpha",
                    lastMessageTid = 1,
                    lastMessageAt = 100
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Group.value,
                    subject = "Beta",
                    lastMessageTid = 2,
                    lastMessageAt = 200
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "",
            types = emptyList(),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchChannelsBySubjectAndTypes_orderedByLastMessageAtDescending() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "First",
                    lastMessageTid = 1,
                    lastMessageAt = 300
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Second",
                    lastMessageTid = 2,
                    lastMessageAt = 100
                ),
                channel(
                    id = 3,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Third",
                    lastMessageTid = 3,
                    lastMessageAt = 200
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(1L, 3L, 2L).inOrder()
    }

    @Test
    fun searchChannelsBySubjectAndTypes_caseInsensitiveSubjectMatch() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Engineering Updates",
                    lastMessageTid = 1,
                    lastMessageAt = 100
                ),
            ),
            links = emptyList(),
        )

        val result = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "engineering",
            types = listOf(ChannelTypeEnum.Public.value),
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(1L)
    }

    private suspend fun insert(vararg channels: ChannelEntity) {
        channelDao.insertChannelsAndLinks(channels.toList(), emptyList())
    }

    private suspend fun insertUsers(vararg users: UserDb) {
        userDao.insertUsersWithMetadata(users.toList())
    }

    private suspend fun insertChannelsAndLinks(
        channels: List<ChannelEntity>,
        links: List<UserChatLinkEntity>,
    ) {
        channelDao.insertChannelsAndLinks(channels, links)
    }

    private fun user(
        id: String,
        firstName: String,
        lastName: String,
        username: String = id,
    ) = UserDb(
        user = UserEntity(
            id = id,
            username = username,
            firstName = firstName,
            lastName = lastName,
            avatarURL = null,
            presence = SceytPresence(PresenceState.Online, "online", 1L),
            activityStatus = UserState.Active,
            blocked = false,
        ),
        metadata = emptyList(),
    )

    private fun channel(
        id: Long,
        type: String = ChannelTypeEnum.Public.value,
        subject: String,
        userRole: String? = "owner",
        lastMessageTid: Long? = id,
        lastMessageAt: Long? = id,
        pending: Boolean = false,
    ) = ChannelEntity(
        id = id,
        parentChannelId = null,
        uri = "channel://$id",
        type = type,
        subject = subject,
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
        lastMessageTid = lastMessageTid,
        lastMessageAt = lastMessageAt,
        pending = pending,
        isSelf = false,
    )

    private fun link(userId: String, channelId: Long, role: String = "member") =
        UserChatLinkEntity(userId = userId, chatId = channelId, role = role)
}
