package com.sceyt.chatuikit.persistence.dao.searchdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.LinkDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SearchAttachmentsDaoTest {

    private lateinit var database: SceytDatabase
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
    fun searchAttachmentsGlobally_blankMediaSearchFiltersBySenderAndExcludesHiddenItems() =
        runTest {
            insertChannelsAndLinks(
                channels = listOf(channel(id = 1, subject = "Media", userRole = "owner")),
                links = emptyList(),
            )
            insertMessages(
                message(
                    tid = 10,
                    id = 301,
                    channelId = 1,
                    body = "photo",
                    fromId = "alice",
                    createdAt = 100
                ),
                message(
                    tid = 11,
                    id = 302,
                    channelId = 1,
                    body = "video",
                    fromId = "alice",
                    createdAt = 200
                ),
                message(
                    tid = 12,
                    id = 303,
                    channelId = 1,
                    body = "other sender",
                    fromId = "bob",
                    createdAt = 300
                ),
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
                attachment(
                    id = 1,
                    messageTid = 10,
                    messageId = 301,
                    channelId = 1,
                    type = AttachmentTypeEnum.Image.value,
                    createdAt = 100
                ),
                attachment(
                    id = 2,
                    messageTid = 11,
                    messageId = 302,
                    channelId = 1,
                    type = AttachmentTypeEnum.Video.value,
                    createdAt = 200
                ),
                attachment(
                    id = 3,
                    messageTid = 12,
                    messageId = 303,
                    channelId = 1,
                    type = AttachmentTypeEnum.Image.value,
                    createdAt = 300
                ),
                attachment(
                    id = 4,
                    messageTid = 13,
                    messageId = 304,
                    channelId = 1,
                    type = AttachmentTypeEnum.Image.value,
                    createdAt = 400
                ),
                attachment(
                    id = 5,
                    messageTid = 14,
                    messageId = 305,
                    channelId = 1,
                    type = AttachmentTypeEnum.Image.value,
                    createdAt = 500
                ),
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

            val result = globalSearchDao.searchAttachments(
                query = "",
                senderId = "alice",
                types = listOf(AttachmentTypeEnum.Image.value, AttachmentTypeEnum.Video.value),
                limit = 20,
                offset = 0,
                queryEmpty = true,
                senderIgnored = false,
                matchAttachmentName = false,
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
            message(
                tid = 20,
                id = 401,
                channelId = 2,
                body = "upload the spec today",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 21,
                id = 402,
                channelId = 2,
                body = "brief is mentioned in body",
                fromId = "alice",
                createdAt = 200
            ),
            message(
                tid = 22,
                id = 403,
                channelId = 2,
                body = "url should not match",
                fromId = "alice",
                createdAt = 300
            ),
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

        val result = globalSearchDao.searchAttachments(
            query = "brief",
            senderId = null,
            types = listOf(AttachmentTypeEnum.File.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
            matchAttachmentName = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(11L, 10L).inOrder()
    }

    @Test
    fun searchAttachmentsGlobally_symbolSeparatedNameMatchesLaterToken() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 2, subject = "Files", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 23,
                id = 404,
                channelId = 2,
                body = "body without match",
                fromId = "alice",
                createdAt = 100,
            ),
            message(
                tid = 24,
                id = 405,
                channelId = 2,
                body = "continuous token",
                fromId = "alice",
                createdAt = 200,
            ),
        )
        insertAttachments(
            attachment(
                id = 13,
                messageTid = 23,
                messageId = 404,
                channelId = 2,
                type = AttachmentTypeEnum.File.value,
                name = "release_notes.pdf",
                createdAt = 100,
            ),
            attachment(
                id = 14,
                messageTid = 24,
                messageId = 405,
                channelId = 2,
                type = AttachmentTypeEnum.File.value,
                name = "releasenotes.pdf",
                createdAt = 200,
            ),
        )

        val result = globalSearchDao.searchAttachments(
            query = "notes",
            senderId = null,
            types = listOf(AttachmentTypeEnum.File.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
            matchAttachmentName = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(13L)
    }

    @Test
    fun searchLinkAttachments_matchesUrl() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 30,
                id = 501,
                channelId = 3,
                body = "body without keyword",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 31,
                id = 502,
                channelId = 3,
                body = "no match here",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://docs.example.com/spec",
                createdAt = 100
            ),
            attachment(
                id = 21,
                messageTid = 31,
                messageId = 502,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/nohit",
                createdAt = 200
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "docs",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(20L)
    }

    @Test
    fun searchLinkAttachments_symbolSeparatedUrlMatchesLaterToken() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 32,
                id = 503,
                channelId = 3,
                body = "body",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 33,
                id = 504,
                channelId = 3,
                body = "body",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 22,
                messageTid = 32,
                messageId = 503,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/docs/example",
                createdAt = 100,
            ),
            attachment(
                id = 23,
                messageTid = 33,
                messageId = 504,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://sample.com/docsexample",
                createdAt = 200,
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "example",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(22L)
    }

    @Test
    fun searchLinkAttachments_matchesLinkDetailsTitle() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 30,
                id = 501,
                channelId = 3,
                body = "irrelevant body",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 31,
                id = 502,
                channelId = 3,
                body = "irrelevant body",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/a",
                createdAt = 100
            ),
            attachment(
                id = 21,
                messageTid = 31,
                messageId = 502,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/b",
                createdAt = 200
            ),
        )
        insertLinkDetails(
            linkDetails(
                link = "https://example.com/a",
                title = "Design System Guide",
                description = null
            ),
            linkDetails(
                link = "https://example.com/b",
                title = "No match here",
                description = null
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "Design",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(20L)
    }

    @Test
    fun searchLinkAttachments_queryPunctuationSplitsIntoTokensForLinkDetails() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 34,
                id = 505,
                channelId = 3,
                body = "body",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 35,
                id = 506,
                channelId = 3,
                body = "body",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 24,
                messageTid = 34,
                messageId = 505,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/a",
                createdAt = 100
            ),
            attachment(
                id = 25,
                messageTid = 35,
                messageId = 506,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/b",
                createdAt = 200
            ),
        )
        insertLinkDetails(
            linkDetails(
                link = "https://example.com/a",
                title = "public channel",
                description = null
            ),
            linkDetails(
                link = "https://example.com/b",
                title = "public update",
                description = null
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "public-channel",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(24L)
    }

    @Test
    fun searchLinkAttachments_matchesLinkDetailsDescription() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 30,
                id = 501,
                channelId = 3,
                body = "irrelevant body",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 31,
                id = 502,
                channelId = 3,
                body = "irrelevant body",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/a",
                createdAt = 100
            ),
            attachment(
                id = 21,
                messageTid = 31,
                messageId = 502,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/b",
                createdAt = 200
            ),
        )
        insertLinkDetails(
            linkDetails(
                link = "https://example.com/a",
                title = null,
                description = "A guide to onboarding flows"
            ),
            linkDetails(
                link = "https://example.com/b",
                title = null,
                description = "Unrelated content"
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "onboarding",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(20L)
    }

    @Test
    fun searchLinkAttachments_blankQueryReturnsAllLinks() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 30,
                id = 501,
                channelId = 3,
                body = "first",
                fromId = "alice",
                createdAt = 100
            ),
            message(
                tid = 31,
                id = 502,
                channelId = 3,
                body = "second",
                fromId = "alice",
                createdAt = 200
            ),
        )
        insertAttachments(
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/a",
                createdAt = 100
            ),
            attachment(
                id = 21,
                messageTid = 31,
                messageId = 502,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/b",
                createdAt = 200
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = true,
            senderIgnored = true,
        )

        assertThat(result.map { it.attachmentEntity.id }).containsExactly(21L, 20L).inOrder()
    }

    @Test
    fun searchLinkAttachments_noFalsePositiveWhenLinkDetailsAbsent() = runTest {
        insertChannelsAndLinks(
            channels = listOf(channel(id = 3, subject = "Links", userRole = "owner")),
            links = emptyList(),
        )
        insertMessages(
            message(
                tid = 30,
                id = 501,
                channelId = 3,
                body = "irrelevant",
                fromId = "alice",
                createdAt = 100
            ),
        )
        insertAttachments(
            // No LinkDetailsEntity inserted — link_details row is NULL after LEFT JOIN
            attachment(
                id = 20,
                messageTid = 30,
                messageId = 501,
                channelId = 3,
                type = AttachmentTypeEnum.Link.value,
                url = "https://example.com/nohit",
                createdAt = 100
            ),
        )

        val result = globalSearchDao.searchLinkAttachments(
            query = "design",
            senderId = null,
            types = listOf(AttachmentTypeEnum.Link.value),
            limit = 20,
            offset = 0,
            queryEmpty = false,
            senderIgnored = true,
        )

        assertThat(result).isEmpty()
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

    private suspend fun insertLinkDetails(vararg details: LinkDetailsEntity) {
        details.forEach { linkDao.insert(it) }
    }

    private fun linkDetails(
        link: String,
        title: String? = null,
        description: String? = null,
    ) = LinkDetailsEntity(
        link = link,
        url = link,
        title = title,
        description = description,
        siteName = null,
        faviconUrl = null,
        imageUrl = null,
        imageWidth = null,
        imageHeight = null,
        thumb = null,
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
