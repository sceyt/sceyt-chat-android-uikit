package com.sceyt.chatuikit.persistence.logicimpl.message

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channel.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class PersistenceMessagesLogicImplRejectedSendTest {
    @Test
    fun `rejected send restores the previous chat preview`() = runBlocking {
        assertPreviewRestored(cacheChannel = true)
    }

    @Test
    fun `rejected send restores the stored preview without a cached channel`() = runBlocking {
        assertPreviewRestored(cacheChannel = false)
    }

    private suspend fun assertPreviewRestored(cacheChannel: Boolean) {
        val context = RuntimeEnvironment.getApplication()
        val database = Room.inMemoryDatabaseBuilder(context, SceytDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val messageDao = database.messageDao()
        val channelDao = database.channelDao()
        val channelsCache = ChannelsCache()
        val messagesCache = MessagesCache()
        val repository = mock<MessagesRepository>()
        val channels = PersistenceChannelsLogicImpl(
            context = context,
            channelsRepository = mock(),
            channelDao = channelDao,
            globalSearchDao = mock(),
            usersDao = mock(),
            messageDao = messageDao,
            rangeDao = mock(),
            draftMessageDao = mock(),
            chatUserReactionDao = mock(),
            pendingReactionDao = mock(),
            channelsCache = channelsCache,
            channelSyncStateStore = mock(),
            pendingChannelCoordinator = mock(),
            insertChannelWithMembersUseCase = mock(),
        )
        val messages = PersistenceMessagesLogicImpl(
            context = context,
            messageDao = messageDao,
            rangeDao = mock(),
            attachmentDao = mock(),
            pendingMarkerDao = mock(),
            reactionDao = mock(),
            userDao = mock(),
            pendingMessageStateDao = mock(),
            pendingMessageDeleteByTidDao = mock(),
            pollDao = mock(),
            pinnedMessageDao = mock(),
            pendingPinDao = mock(),
            pendingPollVoteDao = mock(),
            fileTransferService = mock(),
            messagesRepository = repository,
            preference = mock(),
            messagesCache = messagesCache,
            channelCache = channelsCache,
            messageLoadRangeUpdater = mock(),
            checkDeletedMessagesUseCase = mock(),
            channelSyncStateStore = mock(),
            sendPendingMessagesUseCase = mock(),
        )
        val facade = mock<SceytChatUIFacade>()
        val userInteractor = mock<UserInteractor>()
        whenever(facade.userInteractor).thenReturn(userInteractor)
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<PersistenceChannelsLogic> { channels }
                single { facade }
            })
        }
        try {
            val channelId = 1L
            val previous = createMessage(createdAt = 100L, id = 10L, tid = 11L)
                .copy(channelId = channelId, body = "Previous message")
            val pending = createMessage(createdAt = 200L, tid = 20L).copy(
                channelId = channelId,
                body = "Pending video",
                deliveryStatus = MessageDeliveryStatus.Pending,
                viewOnce = true,
            )
            val channel = createChannel(id = channelId, pinnedAt = 0L, createdAt = 1L, lastMessage = pending)
            channelDao.insertChannelsAndLinks(listOf(channel.toChannelEntity()), emptyList())
            messageDao.upsertMessage(previous.toMessageDb(false))
            messageDao.upsertMessage(pending.toMessageDb(false))
            messagesCache.add(channelId, pending)
            if (cacheChannel) {
                channelsCache.addAll(ChannelListConfig.default, listOf(channel), checkDifference = false)
            }

            val exception = mock<SceytException>()
            whenever(exception.type).thenReturn(SDKErrorTypeEnum.NotAllowed.value)
            val rejection = SceytResponse.Error<SceytMessage>(exception)
            whenever(repository.sendMessage(eq(channelId), any())).thenReturn(rejection)

            val response = messages.sendMessageWithUploadedAttachments(channelId, pending.toMessage())

            assertThat(response).isSameInstanceAs(rejection)
            assertThat(messageDao.getMessageByTid(pending.tid)).isNull()
            assertThat(messagesCache.get(channelId, pending.tid)).isNull()
            assertThat(channels.getChannelFromDb(channelId)?.lastMessage?.tid).isEqualTo(previous.tid)
            if (cacheChannel) {
                assertThat(channelsCache.getOneOf(channelId)?.lastMessage?.body).isEqualTo(previous.body)
            }
        } finally {
            SceytKoinApp.koinApp = null
            stopKoin()
            database.close()
        }
    }
}
