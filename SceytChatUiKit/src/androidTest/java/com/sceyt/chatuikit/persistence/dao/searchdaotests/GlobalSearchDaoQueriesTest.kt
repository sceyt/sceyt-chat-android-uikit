package com.sceyt.chatuikit.persistence.dao.searchdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.SearchMessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
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
class GlobalSearchDaoQueriesTest {

    private lateinit var database: SceytDatabase
    private lateinit var userDao: UserDao
    private lateinit var channelDao: ChannelDao
    private lateinit var messageDao: MessageDao
    private lateinit var searchMessageDao: SearchMessageDao
    private lateinit var attachmentDao: AttachmentDao

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
        searchMessageDao = database.searchMessageDao()
        attachmentDao = database.attachmentsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchUsersLinkedToJoinedChannelsByDisplayName_returnsDistinctJoinedMembersAndExcludesCurrentUser() = runTest {
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
    fun searchUsersLinkedToJoinedChannelsByDisplayName_prioritizesFullNamePrefixBeforeUsernamePrefix() = runTest {
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
    fun searchNonDirectChannelsBySubject_returnsMatchingCachedChannelsRegardlessOfJoinState() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(
                    id = 1,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Design Library",
                    userRole = "",
                    lastMessageTid = 11,
                    lastMessageAt = 300,
                ),
                channel(
                    id = 2,
                    type = ChannelTypeEnum.Group.value,
                    subject = "Design Ops",
                    userRole = "owner",
                    lastMessageTid = 12,
                    lastMessageAt = 200,
                ),
                channel(
                    id = 3,
                    type = ChannelTypeEnum.Direct.value,
                    subject = "Design DM",
                    userRole = "owner",
                    lastMessageTid = 13,
                    lastMessageAt = 400,
                ),
                channel(
                    id = 4,
                    type = ChannelTypeEnum.Public.value,
                    subject = "Design Pending",
                    userRole = "owner",
                    lastMessageTid = 0,
                    pending = true,
                    lastMessageAt = 500,
                ),
            ),
            links = emptyList(),
        )

        val result = channelDao.searchNonDirectChannelsBySubject(
            query = "Design",
            limit = 10,
            offset = 0,
        )

        assertThat(result.map { it.channelEntity.id }).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_filtersBySenderAndExcludesPendingAndUnlisted() = runTest {
        insertChannelsAndLinks(
            channels = listOf(
                channel(id = 1, subject = "Alpha", userRole = "owner"),
                channel(id = 2, subject = "Beta", userRole = "owner"),
            ),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 101, channelId = 1, body = "jam session", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 102, channelId = 2, body = "jam schedule", fromId = "bob", createdAt = 200),
            message(
                tid = 3,
                id = 103,
                channelId = 1,
                body = "jam pending",
                fromId = "alice",
                createdAt = 300,
                deliveryStatus = MessageDeliveryStatus.Pending,
            ),
            message(
                tid = 4,
                id = 104,
                channelId = 1,
                body = "jam hidden",
                fromId = "alice",
                createdAt = 400,
                unList = true,
            ),
        )

        val result = searchMessageDao.searchMessagesGlobally(
            query = "jam",
            senderId = "alice",
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(101L)
    }

    @Test
    fun searchMessagesGlobally_blankQueryReturnsLatestNonPendingMessages() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "Alpha", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 201, channelId = 1, body = "older", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 202, channelId = 1, body = "newer", fromId = "bob", createdAt = 300),
            message(
                tid = 3,
                id = 203,
                channelId = 1,
                body = "pending",
                fromId = "alice",
                createdAt = 500,
                deliveryStatus = MessageDeliveryStatus.Pending,
            ),
        )

        val result = searchMessageDao.searchMessagesGlobally(
            query = "",
            senderId = null,
            limit = 20,
            offset = 0,
        )

        assertThat(result.map { it.messageEntity.id }).containsExactly(202L, 201L).inOrder()
    }

    @Test
    fun searchMessagesGlobally_caseInsensitiveMatchOnSingleWord() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
        )

        val result = searchMessageDao.searchMessagesGlobally(query = "Hello", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_trailingSpacesAreIgnored() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
        )

        val result = searchMessageDao.searchMessagesGlobally(query = "hello       ", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_leadingSpacesAreIgnored() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
        )

        val result = searchMessageDao.searchMessagesGlobally(query = "    hello", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_leadingAndTrailingSpacesAreIgnored() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
        )

        val result = searchMessageDao.searchMessagesGlobally(query = "    hello  ", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multipleInternalSpacesMatchIndividualWords() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "goodbye world", fromId = "alice", createdAt = 200),
        )

        val result = searchMessageDao.searchMessagesGlobally(query = "    hello        my", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multiWordQueryMatchesAllWordsAnywhereInBody() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
            message(tid = 2, id = 2, channelId = 1, body = "hello world", fromId = "alice", createdAt = 200),
        )

        // "hello" and "Matat" both appear in body 1 but only "hello" in body 2
        val result = searchMessageDao.searchMessagesGlobally(query = "hello Matat", senderId = null, limit = 20, offset = 0)

        assertThat(result.map { it.messageEntity.id }).containsExactly(1L)
    }

    @Test
    fun searchMessagesGlobally_multiWordQueryRequiresAllWordsPresent() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "C", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 1, id = 1, channelId = 1, body = "hello my name is Matat", fromId = "alice", createdAt = 100),
        )

        // "missing" is not in the body — should return nothing
        val result = searchMessageDao.searchMessagesGlobally(query = "hello missing", senderId = null, limit = 20, offset = 0)

        assertThat(result).isEmpty()
    }

    @Test
    fun searchAttachmentsGlobally_blankMediaSearchFiltersBySenderAndExcludesHiddenItems() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 1, subject = "Media", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 10, id = 301, channelId = 1, body = "photo", fromId = "alice", createdAt = 100),
            message(tid = 11, id = 302, channelId = 1, body = "video", fromId = "alice", createdAt = 200),
            message(tid = 12, id = 303, channelId = 1, body = "other sender", fromId = "bob", createdAt = 300),
            message(
                tid = 13,
                id = 304,
                channelId = 1,
                body = "pending photo",
                fromId = "alice",
                createdAt = 400,
                deliveryStatus = MessageDeliveryStatus.Pending,
            ),
            message(
                tid = 14,
                id = 305,
                channelId = 1,
                body = "unlisted photo",
                fromId = "alice",
                createdAt = 500,
                unList = true,
            ),
        )
        insertAttachments(
            attachment(id = 1, messageTid = 10, messageId = 301, channelId = 1, type = AttachmentTypeEnum.Image.value, createdAt = 100),
            attachment(id = 2, messageTid = 11, messageId = 302, channelId = 1, type = AttachmentTypeEnum.Video.value, createdAt = 200),
            attachment(id = 3, messageTid = 12, messageId = 303, channelId = 1, type = AttachmentTypeEnum.Image.value, createdAt = 300),
            attachment(id = 4, messageTid = 13, messageId = 304, channelId = 1, type = AttachmentTypeEnum.Image.value, createdAt = 400),
            attachment(id = 5, messageTid = 14, messageId = 305, channelId = 1, type = AttachmentTypeEnum.Image.value, createdAt = 500),
            attachment(
                id = 6,
                messageTid = 11,
                messageId = 302,
                channelId = 1,
                type = AttachmentTypeEnum.Image.value,
                createdAt = 600,
                viewOnce = true,
            ),
        )

        val result = attachmentDao.searchAttachmentsGlobally(
            query = "",
            senderId = "alice",
            types = listOf(AttachmentTypeEnum.Image.value, AttachmentTypeEnum.Video.value),
            limit = 20,
            offset = 0,
            queryEmpty = true,
            senderIgnored = false,
            matchAttachmentName = false,
            matchUrl = false,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun searchAttachmentsGlobally_fileSearchMatchesAttachmentNameAndBodyButNotUrl() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 2, subject = "Files", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 20, id = 401, channelId = 2, body = "upload the spec today", fromId = "alice", createdAt = 100),
            message(tid = 21, id = 402, channelId = 2, body = "brief is mentioned in body", fromId = "alice", createdAt = 200),
            message(tid = 22, id = 403, channelId = 2, body = "url should not match", fromId = "alice", createdAt = 300),
        )
        insertAttachments(
            attachment(
                id = 10,
                messageTid = 20,
                messageId = 401,
                channelId = 2,
                type = AttachmentTypeEnum.File.value,
                name = "spec-brief.pdf",
                url = "https://example.com/nohit",
                createdAt = 100,
            ),
            attachment(
                id = 11,
                messageTid = 21,
                messageId = 402,
                channelId = 2,
                type = AttachmentTypeEnum.File.value,
                name = "notes.pdf",
                url = "https://example.com/nohit",
                createdAt = 200,
            ),
            attachment(
                id = 12,
                messageTid = 22,
                messageId = 403,
                channelId = 2,
                type = AttachmentTypeEnum.File.value,
                name = "notes.pdf",
                url = "https://example.com/brief",
                createdAt = 300,
            ),
        )

        val result = attachmentDao.searchAttachmentsGlobally(
            query = "brief",
            senderId = null,
            types = listOf(AttachmentTypeEnum.File.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
            matchAttachmentName = true,
            matchUrl = false,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(11L, 10L).inOrder()
    }

    @Test
    fun searchAttachmentsGlobally_linkSearchMatchesUrlWhenEnabled() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(tid = 30, id = 501, channelId = 3, body = "body without keyword", fromId = "alice", createdAt = 100),
            message(tid = 31, id = 502, channelId = 3, body = "docs are in body", fromId = "alice", createdAt = 200),
        )
        insertAttachments(
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://docs.example.com/spec",
                createdAt = 100,
            ),
            attachment(
                id = 21,
                messageTid = 31,
                messageId = 502,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/nohit",
                createdAt = 200,
            ),
        )

        val result = attachmentDao.searchAttachmentsGlobally(
            query = "docs",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
            matchAttachmentName = false,
            matchUrl = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(21L, 20L).inOrder()
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

    private suspend fun insertMessages(vararg messages: MessageEntity) {
        messageDao.upsertMessageEntitiesWithTransaction(messages.toList())
    }

    private suspend fun insertAttachments(vararg attachments: AttachmentEntity) {
        attachmentDao.insertAttachments(attachments.toList())
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

    private fun message(
        tid: Long,
        id: Long,
        channelId: Long,
        body: String,
        fromId: String,
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

    private fun attachment(
        id: Long,
        messageTid: Long,
        messageId: Long,
        channelId: Long,
        type: String,
        name: String = "attachment_$id",
        url: String? = "https://example.com/$id",
        createdAt: Long,
        viewOnce: Boolean = false,
    ) = AttachmentEntity(
        id = id,
        messageId = messageId,
        messageTid = messageTid,
        channelId = channelId,
        userId = "sender",
        name = name,
        type = type,
        metadata = null,
        fileSize = 100,
        createdAt = createdAt,
        url = url,
        filePath = "/tmp/$id",
        originalFilePath = null,
        viewOnce = viewOnce,
    )
}
