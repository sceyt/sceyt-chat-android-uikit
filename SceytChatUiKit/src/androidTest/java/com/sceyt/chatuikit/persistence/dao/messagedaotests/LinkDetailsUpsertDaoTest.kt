package com.sceyt.chatuikit.persistence.dao.messagedaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.LinkDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class LinkDetailsUpsertDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var linkDao: LinkDao

    private val link = "https://example.com/article"

    private val partial = LinkDetailsEntity(
        link = link,
        url = link,
        title = "",
        description = null,
        siteName = "",
        faviconUrl = null,
        imageUrl = null,
        imageWidth = null,
        imageHeight = null,
        thumb = null
    )

    private val full = LinkDetailsEntity(
        link = link,
        url = link,
        title = "Title",
        description = "Description",
        siteName = "Example",
        faviconUrl = "https://example.com/favicon.ico",
        imageUrl = "https://example.com/image.png",
        imageWidth = 1200,
        imageHeight = 630,
        thumb = null
    )

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
        linkDao = database.linkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun laterFullLinkDetails_fillIncompleteStoredRow() = runTest {
        messageDao.upsertMessage(messageWithLink(tid = 1, details = partial))
        messageDao.upsertMessage(messageWithLink(tid = 2, details = full))

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(full)
    }

    @Test
    fun laterIncompleteLinkDetails_keepStoredValuesAndThumb() = runTest {
        messageDao.upsertMessage(messageWithLink(tid = 1, details = full))
        linkDao.updateThumb(link, "thumb")
        messageDao.upsertMessage(messageWithLink(tid = 2, details = partial))

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(full.copy(thumb = "thumb"))
    }

    @Test
    fun laterMessage_doesNotOverwriteStoredValues() = runTest {
        messageDao.upsertMessage(messageWithLink(tid = 1, details = full))
        val other = full.copy(title = "Other title", imageUrl = "https://other.example.com/image.png")
        messageDao.upsertMessage(messageWithLink(tid = 2, details = other))

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(full)
    }

    @Test
    fun sameLinkInOneBatch_mergesIntoOneCompleteRow() = runTest {
        messageDao.upsertMessages(
            listOf(
                messageWithLink(tid = 1, details = partial),
                messageWithLink(tid = 2, details = full),
                messageWithLink(tid = 3, details = partial)
            )
        )

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(full)
    }

    @Test
    fun linkDaoUpsert_newerTextWinsAndKeepsStoredImageData() = runTest {
        linkDao.insert(full.copy(thumb = "thumb"))
        val sent = full.copy(title = "New title", imageWidth = null, imageHeight = null, thumb = null)
        linkDao.upsert(sent)

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(full.copy(title = "New title", thumb = "thumb"))
    }

    @Test
    fun linkDaoUpsert_changedImageDoesNotKeepStoredImageData() = runTest {
        linkDao.insert(full.copy(thumb = "thumb"))
        val newer = full.copy(
            imageUrl = "https://example.com/new.png",
            imageWidth = null,
            imageHeight = null,
            thumb = null
        )
        linkDao.upsert(newer)

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(newer)
    }

    @Test
    fun linkDaoUpsert_changedImageWithSizeDoesNotKeepStoredThumb() = runTest {
        linkDao.insert(full.copy(thumb = "thumb"))
        val newer = full.copy(imageUrl = "https://example.com/new.png", imageWidth = 400, imageHeight = 400)
        linkDao.upsert(newer)

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(newer)
    }

    @Test
    fun laterMessage_doesNotFillStoredImageDataFromDifferentImage() = runTest {
        val stored = full.copy(imageWidth = null, imageHeight = null)
        messageDao.upsertMessage(messageWithLink(tid = 1, details = stored))
        val other = full.copy(imageUrl = "https://example.com/new.png", thumb = "otherThumb")
        messageDao.upsertMessage(messageWithLink(tid = 2, details = other))

        assertThat(linkDao.getLinkDetailsEntity(link)).isEqualTo(stored)
    }

    private fun messageWithLink(tid: Long, details: LinkDetailsEntity) = MessageDb(
        messageEntity = message(tid),
        from = null,
        parent = null,
        attachments = listOf(
            AttachmentDb(
                attachmentEntity = AttachmentEntity(
                    id = tid,
                    messageId = tid,
                    messageTid = tid,
                    channelId = 1L,
                    userId = "user1",
                    name = details.title.orEmpty(),
                    type = AttachmentTypeEnum.Link.value,
                    metadata = null,
                    fileSize = 0,
                    createdAt = tid,
                    url = link,
                    filePath = null,
                    originalFilePath = null,
                    viewOnce = false
                ),
                payLoad = null,
                linkDetails = details
            )
        ),
        userMarkers = null,
        reactions = null,
        reactionsTotals = null,
        pendingReactions = null,
        forwardingUser = null,
        mentionedUsers = null,
        poll = null,
    )

    private fun message(tid: Long) = MessageEntity(
        tid = tid,
        id = tid,
        channelId = 1L,
        body = link,
        type = "text",
        metadata = null,
        createdAt = tid,
        updatedAt = 0,
        incoming = true,
        isTransient = false,
        silent = false,
        deliveryStatus = MessageDeliveryStatus.Displayed,
        state = MessageState.Unmodified,
        fromId = "user1",
        markerCount = null,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0L,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        unList = false,
        disableMentionsCount = false,
        viewOnce = false,
    )
}
