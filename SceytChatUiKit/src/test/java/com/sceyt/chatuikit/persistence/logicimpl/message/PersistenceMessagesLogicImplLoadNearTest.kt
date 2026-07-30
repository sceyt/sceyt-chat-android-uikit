package com.sceyt.chatuikit.persistence.logicimpl.message

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMarkerDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageDeleteByTidDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CheckDeletedMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class PersistenceMessagesLogicImplLoadNearTest {

    private val messageDao = mock<MessageDao>()
    private val rangeDao = mock<LoadRangeDao>()
    private val messagesCache = mock<MessagesCache>()
    private val channelCache = mock<ChannelsCache>()

    @Test
    fun `load near db response excludes pending messages when window does not contain last message`() = runTest {
        val lastMessage = message(id = 100, createdAt = 100)
        val nearMessages = listOf(
            message(id = 40, createdAt = 40).toDb(),
            message(id = 50, createdAt = 50).toDb(),
        )
        stubLoadNear(lastMessage = lastMessage, messageId = 50, nearMessages = nearMessages)
        whenever(messageDao.getPendingMessages(CHANNEL_ID)).thenReturn(
            listOf(pendingMessage(tid = 900, createdAt = 900).toDb())
        )

        val response = logic().loadNearMessages(
            conversationId = CHANNEL_ID,
            messageId = 50,
            replyInThread = false,
            limit = LIMIT,
            loadKey = LoadKeyData(),
            ignoreDb = false,
            ignoreServer = true
        ).first() as PaginationResponse.DBResponse

        assertThat(response.data.map { it.id }).containsExactly(40L, 50L).inOrder()
    }

    @Test
    fun `load near db response includes pending messages when window contains last message`() = runTest {
        val lastMessage = message(id = 100, createdAt = 100)
        val pendingMessage = pendingMessage(tid = 900, createdAt = 900)
        stubLoadNear(
            lastMessage = lastMessage,
            messageId = 100,
            nearMessages = listOf(
                message(id = 90, createdAt = 90).toDb(),
                lastMessage.toDb(),
            )
        )
        whenever(messageDao.getPendingMessages(CHANNEL_ID)).thenReturn(listOf(pendingMessage.toDb()))

        val response = logic().loadNearMessages(
            conversationId = CHANNEL_ID,
            messageId = 100,
            replyInThread = false,
            limit = LIMIT,
            loadKey = LoadKeyData(),
            ignoreDb = false,
            ignoreServer = true
        ).first() as PaginationResponse.DBResponse

        assertThat(response.data.map { it.id }).containsExactly(90L, 100L, 0L).inOrder()
    }

    private suspend fun stubLoadNear(
        lastMessage: SceytMessage,
        messageId: Long,
        nearMessages: List<MessageDb>,
    ) {
        whenever(messageDao.getOutdatedMessageTIds(any(), any())).thenReturn(emptyList())
        whenever(messageDao.getNearMessages(CHANNEL_ID, messageId, LIMIT))
            .thenReturn(LoadNearData(nearMessages, hasNext = false, hasPrev = false))
        whenever(channelCache.getOneOf(CHANNEL_ID)).thenReturn(
            createChannel(
                id = CHANNEL_ID,
                pinnedAt = 0,
                createdAt = 0,
                lastMessage = lastMessage
            )
        )
    }

    private fun logic() = PersistenceMessagesLogicImpl(
        context = mock<Context>(),
        messageDao = messageDao,
        rangeDao = rangeDao,
        attachmentDao = mock<AttachmentDao>(),
        pendingMarkerDao = mock<PendingMarkerDao>(),
        reactionDao = mock<ReactionDao>(),
        userDao = mock<UserDao>(),
        pendingMessageStateDao = mock<PendingMessageStateDao>(),
        pollDao = mock<PollDao>(),
        pendingPollVoteDao = mock<PendingPollVoteDao>(),
        fileTransferService = mock<FileTransferService>(),
        messagesRepository = mock<MessagesRepository>(),
        preference = mock<SceytSharedPreference>(),
        messagesCache = messagesCache,
        channelCache = channelCache,
        messageLoadRangeUpdater = MessageLoadRangeUpdater(rangeDao),
        checkDeletedMessagesUseCase = mock<CheckDeletedMessagesUseCase>(),
        channelSyncStateStore = mock<ChannelSyncStateStore>(),
        pendingMessageDeleteByTidDao = mock<PendingMessageDeleteByTidDao>(),
    )

    private fun message(id: Long, createdAt: Long): SceytMessage {
        return createMessage(createdAt = createdAt, id = id, tid = id)
            .copy(channelId = CHANNEL_ID)
    }

    private fun pendingMessage(tid: Long, createdAt: Long): SceytMessage {
        return createMessage(createdAt = createdAt, id = 0, tid = tid)
            .copy(
                channelId = CHANNEL_ID,
                deliveryStatus = MessageDeliveryStatus.Pending
            )
    }

    private fun SceytMessage.toDb(): MessageDb = toMessageDb(unList = false)

    private companion object {
        const val CHANNEL_ID = 42L
        const val LIMIT = 10
    }
}
