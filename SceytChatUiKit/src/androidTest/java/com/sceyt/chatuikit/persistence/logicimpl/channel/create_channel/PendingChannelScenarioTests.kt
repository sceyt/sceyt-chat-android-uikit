package com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SearchChannelParams
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channel.PendingChannelMigrationLock
import com.sceyt.chatuikit.persistence.logicimpl.channel.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CreatePendingChannelUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindExistingChannelByMembersUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindRealChannelForPendingUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MergePendingDirectChannelsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MigratePendingChannelToRealChannelUseCase
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@SmallTest
class PendingChannelScenarioTests : SceytKoinComponent {
    private lateinit var channelsRepository: FakeChannelsRepository
    private lateinit var channelLogic: PersistenceChannelsLogic
    private lateinit var channelDao: ChannelDao
    private lateinit var messageDao: MessageDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var messagesCache: MessagesCache
    private lateinit var channelsCache: ChannelsCache

    private val currentUser = User("marat")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        SceytChatUIKit.initialize(
            appContext = ApplicationProvider.getApplicationContext(),
            clientId = UUID.randomUUID().toString(),
            appId = "test-app-id",
            apiUrl = "https://example.com",
            enableDatabase = false
        )
        ClientWrapper.currentUser = currentUser

        channelsRepository = FakeChannelsRepository()
        channelDao = getKoin().get()
        messageDao = getKoin().get()
        attachmentDao = getKoin().get()
        messagesCache = getKoin().get()
        channelsCache = getKoin().get()
        channelLogic = createChannelLogic(channelsRepository)
    }

    @After
    fun tearDown() {
        ClientWrapper.currentUser = null
    }

    @Test
    fun syncChannels_shouldMergePendingDirectChannelAndMoveLocalState() = runTest {
        val peerId = "sync-peer"
        val pendingChannel = createPendingDirect(peerId)
        seedLocalState(
            channelId = pendingChannel.id,
            messageTid = 9001,
            draftBody = "sync draft"
        )
        val realChannel = directChannel(id = 5001, peerId = peerId)
        channelsRepository.syncResults = listOf(
            SyncResult.Proportion(listOf(realChannel)),
            SyncResult.SuccessfullyFinished
        )

        channelLogic.syncChannels(directConfig()).toList()

        assertMergedPendingChannel(
            pendingChannelId = pendingChannel.id,
            realChannelId = realChannel.id,
            movedMessageTid = 9001,
            draftBody = "sync draft"
        )
    }

    @Test
    fun createNewChannelInsteadOfPendingChannel_shouldMigratePendingLocalStateFromCreateResponse() = runTest {
        val peerId = "create-peer"
        val pendingChannel = createPendingDirect(peerId)
        seedLocalState(
            channelId = pendingChannel.id,
            messageTid = 9002,
            draftBody = "create draft"
        )
        val realChannel = directChannel(id = 5002, peerId = peerId)
        channelsRepository.createChannelResponse = SceytResponse.Success(realChannel)

        val response = channelLogic.createNewChannelInsteadOfPendingChannel(pendingChannel)

        assertThat(response.successData().id).isEqualTo(realChannel.id)
        assertMergedPendingChannel(
            pendingChannelId = pendingChannel.id,
            realChannelId = realChannel.id,
            movedMessageTid = 9002,
            draftBody = "create draft"
        )
    }

    @Test
    fun createNewChannelInsteadOfPendingChannel_shouldUseSyncedRealChannelWhenSyncFinishesBeforeCreateResponse() = runTest {
        val peerId = "sync-before-create-peer"
        val pendingChannel = createPendingDirect(peerId)
        seedLocalState(
            channelId = pendingChannel.id,
            messageTid = 9003,
            draftBody = "race draft"
        )
        val realChannel = directChannel(id = 5003, peerId = peerId)
        val releaseCreateResponse = CompletableDeferred<Unit>()
        channelsRepository.createChannelResponse = SceytResponse.Success(realChannel)
        channelsRepository.releaseCreateResponse = releaseCreateResponse
        channelsRepository.syncResults = listOf(
            SyncResult.Proportion(listOf(realChannel)),
            SyncResult.SuccessfullyFinished
        )

        val createResponse = async {
            channelLogic.createNewChannelInsteadOfPendingChannel(pendingChannel)
        }
        channelsRepository.createStarted.await()

        withContext(Dispatchers.Default) {
            withTimeout(2_000.milliseconds) {
                channelLogic.syncChannels(directConfig()).toList()
            }
        }

        assertThat(channelLogic.getChannelFromDb(pendingChannel.id)).isNull()
        releaseCreateResponse.complete(Unit)

        assertThat(createResponse.await().successData().id).isEqualTo(realChannel.id)
        assertMergedPendingChannel(
            pendingChannelId = pendingChannel.id,
            realChannelId = realChannel.id,
            movedMessageTid = 9003,
            draftBody = "race draft"
        )
        assertThat(messageDao.getPendingMessages(realChannel.id).map { it.messageEntity.tid })
            .containsExactly(9003L)
    }

    @Test
    fun concurrentFindOrCreateAndSync_shouldConvergeToSingleRealDirectChannel() = runTest {
        val peerId = "concurrent-peer"
        val realChannel = directChannel(id = 5004, peerId = peerId)
        channelsRepository.syncResults = listOf(
            SyncResult.Proportion(listOf(realChannel)),
            SyncResult.SuccessfullyFinished
        )

        val createResults = (1..5).map {
            async { createPendingDirect(peerId) }
        }
        val syncResult = async {
            channelLogic.syncChannels(directConfig()).toList()
        }

        createResults.awaitAll()
        syncResult.await()

        val repeatedFind = createPendingDirect(peerId)
        assertThat(repeatedFind.id).isEqualTo(realChannel.id)
        assertThat(channelDao.getPendingChannelsByType(ChannelTypeEnum.Direct.value)).isEmpty()
        assertThat(channelLogic.getChannelFromDb(realChannel.id)).isNotNull()
    }

    @Test
    fun getChannelFromServerByUri_shouldMigratePendingUriChannelWhenServerReturnsRealChannel() = runTest {
        val uri = "scenario-uri"
        val peerId = "uri-peer"
        val createData = CreateChannelData(
            type = ChannelTypeEnum.Direct.value,
            uri = uri,
            members = listOf(SceytMember(SceytUser(peerId), "participant"))
        )
        val pendingChannel = channelLogic.findOrCreatePendingChannelByUri(createData).successData()
        seedLocalState(
            channelId = pendingChannel.id,
            messageTid = 9004,
            draftBody = "uri draft"
        )
        val realChannel = directChannel(id = 5005, peerId = peerId, uri = uri)
        channelsRepository.channelByUri = realChannel

        val response = channelLogic.getChannelFromServerByUri(uri)

        assertThat(response.successData()?.id).isEqualTo(realChannel.id)
        assertMergedPendingChannel(
            pendingChannelId = pendingChannel.id,
            realChannelId = realChannel.id,
            movedMessageTid = 9004,
            draftBody = "uri draft"
        )
    }

    private fun createChannelLogic(repository: ChannelsRepository): PersistenceChannelsLogic {
        return PersistenceChannelsLogicImpl(
            context = ApplicationProvider.getApplicationContext(),
            channelsRepository = repository,
            channelDao = channelDao,
            globalSearchDao = getKoin().get<GlobalSearchDao>(),
            usersDao = getKoin().get<UserDao>(),
            messageDao = messageDao,
            rangeDao = getKoin().get<LoadRangeDao>(),
            draftMessageDao = getKoin().get<DraftMessageDao>(),
            chatUserReactionDao = getKoin().get<ChatUserReactionDao>(),
            pendingReactionDao = getKoin().get<PendingReactionDao>(),
            channelsCache = channelsCache,
            channelSyncStateStore = getKoin().get<ChannelSyncStateStore>(),
            pendingChannelMigrationLock = getKoin().get<PendingChannelMigrationLock>(),
            findExistingChannelByMembersUseCase = getKoin().get<FindExistingChannelByMembersUseCase>(),
            createPendingChannelUseCase = getKoin().get<CreatePendingChannelUseCase>(),
            findRealChannelForPendingUseCase = getKoin().get<FindRealChannelForPendingUseCase>(),
            migratePendingChannelToRealChannelUseCase = getKoin().get<MigratePendingChannelToRealChannelUseCase>(),
            mergePendingDirectChannelsUseCase = getKoin().get<MergePendingDirectChannelsUseCase>()
        )
    }

    private suspend fun createPendingDirect(peerId: String): SceytChannel {
        return channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(SceytUser(peerId), "participant"))
            )
        ).successData()
    }

    private suspend fun seedLocalState(
        channelId: Long,
        messageTid: Long,
        draftBody: String,
    ) {
        val message = pendingMessage(channelId, messageTid)
        messageDao.upsertMessage(message.toMessageDb(false))
        attachmentDao.insertAttachments(listOf(attachment(channelId, messageTid)))
        messagesCache.add(channelId, message)
        channelDao.updateLastMessage(
            channelId = channelId,
            lastMessageTid = message.tid,
            lastMessageAt = message.createdAt
        )
        channelLogic.updateDraftMessage(
            DraftMessage(
                channelId = channelId,
                body = draftBody,
                createdAt = message.createdAt,
                mentionUsers = null,
                replyOrEditMessage = null,
                isReply = false,
                bodyAttributes = null,
                attachments = null,
                voiceAttachment = null,
                viewOnce = false
            )
        )
    }

    private suspend fun assertMergedPendingChannel(
        pendingChannelId: Long,
        realChannelId: Long,
        movedMessageTid: Long,
        draftBody: String,
    ) {
        assertThat(channelLogic.getChannelFromDb(pendingChannelId)).isNull()
        val realChannel = channelLogic.getChannelFromDb(realChannelId)
        assertThat(realChannel).isNotNull()
        assertThat(realChannel!!.pending).isFalse()
        assertThat(realChannel.draftMessage?.body).isEqualTo(draftBody)
        assertThat(channelsCache.getRealChannelIdWithPendingChannelId(pendingChannelId)).isEqualTo(realChannelId)

        val movedMessages = messageDao.getPendingMessages(realChannelId)
        assertThat(movedMessages.map { it.messageEntity.tid }).contains(movedMessageTid)
        assertThat(messageDao.getPendingMessages(pendingChannelId)).isEmpty()
        assertThat(messagesCache.get(realChannelId, movedMessageTid)?.channelId).isEqualTo(realChannelId)
        assertThat(messagesCache.get(pendingChannelId, movedMessageTid)).isNull()

        val movedAttachments = attachmentDao.getNewestThenAttachmentInclude(
            channelId = realChannelId,
            attachmentId = 7001,
            limit = 10,
            types = listOf("image")
        )
        assertThat(movedAttachments.map { it.attachmentEntity.messageTid }).contains(movedMessageTid)
    }

    private fun directConfig(): ChannelListConfig {
        return ChannelListConfig.default.copy(
            types = listOf(ChannelTypeEnum.Direct.value),
            queryLimit = 20
        )
    }

    private fun directChannel(
        id: Long,
        peerId: String,
        uri: String? = null,
    ): SceytChannel {
        return SceytChannel(
            id = id,
            parentChannelId = null,
            uri = uri,
            type = ChannelTypeEnum.Direct.value,
            subject = "",
            avatarUrl = null,
            metadata = "",
            createdAt = id,
            updatedAt = 0,
            messagesClearedAt = 0,
            memberCount = 1,
            createdBy = currentUser.toSceytUser(),
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
            lastMessage = null,
            messages = null,
            members = listOf(SceytMember(SceytUser(peerId), "participant")),
            newReactions = null,
            pendingReactions = null,
            pending = false,
            draftMessage = null,
            events = null
        )
    }

    private fun pendingMessage(channelId: Long, tid: Long): SceytMessage {
        return SceytMessage(
            id = 0,
            tid = tid,
            channelId = channelId,
            body = "pending message",
            type = "text",
            metadata = null,
            createdAt = tid,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            viewOnce = false,
            deliveryStatus = MessageDeliveryStatus.Pending,
            state = com.sceyt.chat.models.message.MessageState.Unmodified,
            user = currentUser.toSceytUser(),
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

    private fun attachment(channelId: Long, messageTid: Long) = AttachmentEntity(
        id = 7001,
        messageId = 0,
        messageTid = messageTid,
        channelId = channelId,
        userId = currentUser.id,
        name = "image.png",
        type = "image",
        metadata = null,
        fileSize = 100,
        createdAt = 20,
        url = "https://example.com/image.png",
        filePath = null,
        originalFilePath = null,
        viewOnce = false
    )

    private fun <T> SceytResponse<T>.successData(): T {
        assertThat(this).isInstanceOf(SceytResponse.Success::class.java)
        return data!!
    }

    private class FakeChannelsRepository : ChannelsRepository {
        var syncResults: List<SyncResult<SceytChannel>> = emptyList()
        var createChannelResponse: SceytResponse<SceytChannel> = SceytResponse.Error()
        var channelByUri: SceytChannel? = null
        var releaseCreateResponse: CompletableDeferred<Unit>? = null
        val createStarted = CompletableDeferred<Unit>()

        override suspend fun getChannel(id: Long): SceytResponse<SceytChannel> = unused()

        override suspend fun getChannelByUri(uri: String): SceytResponse<SceytChannel?> {
            return SceytResponse.Success(channelByUri)
        }

        override suspend fun getChannelByInviteKey(inviteKey: String): SceytResponse<SceytChannel> = unused()

        override suspend fun getChannels(
            query: String,
            config: ChannelListConfig,
            params: SearchChannelParams
        ): SceytResponse<List<SceytChannel>> = unused()

        override suspend fun loadMoreChannels(
            query: String,
            config: ChannelListConfig,
            params: SearchChannelParams
        ): SceytResponse<List<SceytChannel>> = unused()

        override suspend fun getAllChannels(limit: Int): Flow<SyncResult<SceytChannel>> = flow {
            syncResults.forEach { emit(it) }
        }

        override suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel> {
            createStarted.complete(Unit)
            releaseCreateResponse?.await()
            return createChannelResponse
        }

        override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> = unused()
        override suspend fun clearHistory(
            channelId: Long,
            forEveryone: Boolean
        ): SceytResponse<SceytChannel> = unused()
        override suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun unHideChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun blockChannel(channelId: Long): SceytResponse<Long> = unused()
        override suspend fun unBlockChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun uploadAvatar(avatarUri: String): SceytResponse<String> = unused()
        override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> = unused()
        override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> = unused()

        override suspend fun loadChannelMembers(
            channelId: Long,
            nextToken: String,
            role: String?
        ): SceytPagingResponse<List<SceytMember>> = unused()

        override suspend fun addMembersToChannel(
            channelId: Long,
            members: List<Member>
        ): SceytResponse<SceytChannel> = unused()

        override suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel> = unused()

        override suspend fun changeChannelMemberRole(
            channelId: Long,
            vararg member: Member
        ): SceytResponse<SceytChannel> = unused()

        override suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> = unused()
        override suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> = unused()
        override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun enableAutoDelete(channelId: Long, period: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun join(channelId: Long): SceytResponse<SceytChannel> = unused()
        override suspend fun joinWithInviteKey(inviteKey: String): SceytResponse<SceytChannel> = unused()
        override suspend fun sendChannelEvent(channelId: Long, event: String) = unused<Unit>()
        override suspend fun getCommonGroups(userId: String): SceytPagingResponse<List<SceytChannel>> = unused()
        override suspend fun loadMoreCommonGroups(): SceytPagingResponse<List<SceytChannel>> = unused()

        private fun <T> unused(): T = error("Not used in pending channel scenario tests")
    }
}
