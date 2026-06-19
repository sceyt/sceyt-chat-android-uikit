package com.sceyt.chatuikit.persistence.logicimpl.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@SmallTest
class PendingChannelUseCasesTest {
    private lateinit var database: SceytDatabase
    private lateinit var channelsCache: ChannelsCache
    private lateinit var messagesCache: MessagesCache
    private lateinit var syncStateStore: ChannelSyncStateStore
    private lateinit var createPendingChannelUseCase: CreatePendingChannelUseCase
    private lateinit var findExistingChannelByMembersUseCase: FindExistingChannelByMembersUseCase
    private lateinit var migratePendingChannelToRealChannelUseCase: MigratePendingChannelToRealChannelUseCase
    private lateinit var mergePendingDirectChannelsUseCase: MergePendingDirectChannelsUseCase

    private val currentUser = User("me")

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

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()

        channelsCache = ChannelsCache()
        messagesCache = MessagesCache()
        syncStateStore = ChannelSyncStateStore(database.channelSyncStateDao())
        createPendingChannelUseCase = CreatePendingChannelUseCase(
            channelDao = database.channelDao(),
            usersDao = database.userDao(),
            channelsCache = channelsCache
        )
        findExistingChannelByMembersUseCase = FindExistingChannelByMembersUseCase(database.channelDao())
        migratePendingChannelToRealChannelUseCase = MigratePendingChannelToRealChannelUseCase(
            channelDao = database.channelDao(),
            messageDao = database.messageDao(),
            attachmentDao = database.attachmentsDao(),
            rangeDao = database.loadRangeDao(),
            draftMessageDao = database.draftMessageDao(),
            pendingReactionDao = database.pendingReactionDao(),
            pendingMessageStateDao = database.pendingMessageStateDao(),
            channelsCache = channelsCache,
            messagesCache = messagesCache,
            channelSyncStateStore = syncStateStore
        )
        mergePendingDirectChannelsUseCase = MergePendingDirectChannelsUseCase(
            channelDao = database.channelDao(),
            migratePendingChannelToRealChannelUseCase = migratePendingChannelToRealChannelUseCase
        )
    }

    @After
    fun tearDown() {
        database.close()
        ClientWrapper.currentUser = null
    }

    @Test
    fun createPendingChannel_shouldNormalizeDuplicateMembersAndKeepStableDirectId() = runTest {
        val peerOnly = createPendingChannelUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(member("peer"))
            ),
            currentUserId = currentUser.id
        ).successData()

        val withCurrentUserAndDuplicatePeer = createPendingChannelUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(member("peer"), member(currentUser.id), member("peer", role = "admin"))
            ),
            currentUserId = currentUser.id
        ).successData()

        assertThat(withCurrentUserAndDuplicatePeer.id).isEqualTo(peerOnly.id)
        assertThat(withCurrentUserAndDuplicatePeer.members!!.map { it.id }).containsExactly(currentUser.id, "peer")
        assertThat(channelsCache.isPending(withCurrentUserAndDuplicatePeer.id)).isTrue()
    }

    @Test
    fun findExistingChannelByMembers_shouldFindDirectForPeerOnlyAndSelfChannel() = runTest {
        val realDirect = channel(id = 1001, type = ChannelTypeEnum.Direct.value, members = listOf(member("peer")))
        insertChannel(realDirect)
        val realSelf = channel(
            id = 1002,
            type = ChannelTypeEnum.Direct.value,
            members = listOf(member(currentUser.id)),
            metadata = Gson().toJson(SelfChannelMetadata(1))
        )
        insertChannel(realSelf)

        val foundPeerOnly = findExistingChannelByMembersUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(member("peer"), member("peer", role = "admin"))
            ),
            currentUserId = currentUser.id
        )
        val foundSelf = findExistingChannelByMembersUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(member(currentUser.id))
            ),
            currentUserId = currentUser.id
        )

        assertThat(foundPeerOnly?.id).isEqualTo(realDirect.id)
        assertThat(foundSelf?.id).isEqualTo(realSelf.id)
    }

    @Test
    fun mergePendingDirectChannels_shouldMergeMatchingDirectAndIgnoreGroup() = runTest {
        val pendingDirect = createPendingChannelUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(member("peer"))
            ),
            currentUserId = currentUser.id
        ).successData()
        val realDirect = channel(id = 2001, type = ChannelTypeEnum.Direct.value, members = listOf(member("peer")))
        insertChannel(realDirect)

        val pendingGroup = createPendingChannelUseCase(
            data = CreateChannelData(
                type = ChannelTypeEnum.Group.value,
                subject = "pending group",
                members = listOf(member(currentUser.id), member("group-peer"))
            ),
            currentUserId = currentUser.id
        ).successData()
        val realGroup = channel(
            id = 2002,
            type = ChannelTypeEnum.Group.value,
            members = listOf(member(currentUser.id), member("group-peer"))
        )
        insertChannel(realGroup)

        val mergedChannels = mergePendingDirectChannelsUseCase(listOf(realDirect, realGroup), currentUser.id)

        assertThat(mergedChannels.first { it.id == realDirect.id }.pending).isFalse()
        assertThat(database.channelDao().getChannelById(pendingDirect.id)).isNull()
        assertThat(database.channelDao().getChannelById(realDirect.id)).isNotNull()
        assertThat(database.channelDao().getChannelById(pendingGroup.id)).isNotNull()
        assertThat(database.channelDao().getChannelById(realGroup.id)).isNotNull()
    }

    @Test
    fun migratePendingChannelToRealChannel_shouldMoveLocalStateAndCleanupPendingChannel() = runTest {
        val pendingMessage = message(
            id = 101,
            tid = 9001,
            channelId = 3001,
            createdAt = 500,
            deliveryStatus = MessageDeliveryStatus.Pending
        )
        val pendingChannel = channel(
            id = 3001,
            type = ChannelTypeEnum.Direct.value,
            members = listOf(member("peer")),
            pending = true,
            lastMessage = pendingMessage
        )
        val realChannel = channel(
            id = 3002,
            type = ChannelTypeEnum.Direct.value,
            members = listOf(member("peer")),
            lastMessage = message(id = 99, tid = 99, channelId = 3002, createdAt = 100)
        )
        insertChannel(pendingChannel)
        insertChannel(realChannel)
        database.messageDao().upsertMessage(pendingMessage.toMessageDb(false))
        database.attachmentsDao().insertAttachments(listOf(attachment(channelId = pendingChannel.id, messageTid = pendingMessage.tid)))
        messagesCache.add(pendingChannel.id, pendingMessage)
        database.draftMessageDao().insert(DraftMessageEntity(pendingChannel.id, "pending draft", 20, null, false, null, false))
        database.draftMessageDao().insert(DraftMessageEntity(realChannel.id, "old real draft", 10, null, false, null, false))
        database.pendingReactionDao().insertIfMessageExist(
            PendingReactionEntity(
                messageId = pendingMessage.id,
                key = "like",
                score = 1,
                reason = "",
                enforceUnique = false,
                count = 1,
                channelId = pendingChannel.id,
                isAdd = true,
                createdAt = 1,
                incomingMsg = false
            )
        )
        database.pendingMessageStateDao().insert(
            PendingMessageStateEntity(
                messageId = pendingMessage.id,
                channelId = pendingChannel.id,
                state = MessageState.Deleted,
                editBody = null,
                deleteOnlyForMe = false
            )
        )
        database.loadRangeDao().insert(LoadRangeEntity(1, 10, pendingChannel.id))
        syncStateStore.updateSyncState(pendingChannel.id, 55)

        val mergedChannel = migratePendingChannelToRealChannelUseCase(pendingChannel, realChannel)

        assertThat(database.channelDao().getChannelById(pendingChannel.id)).isNull()
        assertThat(database.messageDao().getPendingMessages(realChannel.id).map { it.messageEntity.tid })
            .contains(pendingMessage.tid)
        assertThat(database.messageDao().getPendingMessages(pendingChannel.id)).isEmpty()
        assertThat(messagesCache.get(realChannel.id, pendingMessage.tid)?.channelId).isEqualTo(realChannel.id)
        assertThat(messagesCache.get(pendingChannel.id, pendingMessage.tid)).isNull()
        assertThat(
            database.attachmentsDao()
                .getNewestThenAttachmentInclude(realChannel.id, 7001, 10, listOf("image"))
                .map { it.attachmentEntity.channelId }
        ).contains(realChannel.id)
        assertThat(database.draftMessageDao().getDraftByChannelId(realChannel.id)?.draftMessageEntity?.message)
            .isEqualTo("pending draft")
        assertThat(database.draftMessageDao().getDraftByChannelId(pendingChannel.id)).isNull()
        assertThat(database.pendingReactionDao().getAllByChannelId(realChannel.id)).hasSize(1)
        assertThat(database.pendingMessageStateDao().getAll().single().channelId).isEqualTo(realChannel.id)
        assertThat(database.loadRangeDao().getAll(pendingChannel.id)).isEmpty()
        assertThat(database.channelSyncStateDao().getLastSyncedMessageId(pendingChannel.id)).isNull()
        assertThat(mergedChannel.lastMessage?.tid).isEqualTo(pendingMessage.tid)
        assertThat(channelsCache.getRealChannelIdWithPendingChannelId(pendingChannel.id)).isEqualTo(realChannel.id)
    }

    private suspend fun insertChannel(channel: SceytChannel) {
        database.channelDao().insertChannelAndLinks(
            channel = channel.toChannelEntity(),
            userChatLinks = channel.members.orEmpty().map {
                UserChatLinkEntity(userId = it.id, chatId = channel.id, role = it.role.name)
            }
        )
    }

    private fun member(id: String, role: String = "owner") = SceytMember(SceytUser(id), role)

    private fun channel(
        id: Long,
        type: String,
        members: List<SceytMember>,
        metadata: String = "",
        pending: Boolean = false,
        lastMessage: SceytMessage? = null,
    ) = SceytChannel(
        id = id,
        parentChannelId = null,
        uri = null,
        type = type,
        subject = "",
        avatarUrl = null,
        metadata = metadata,
        createdAt = id,
        updatedAt = 0,
        messagesClearedAt = 0,
        memberCount = members.size.toLong(),
        createdBy = SceytUser(currentUser.id),
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
        lastMessage = lastMessage,
        messages = null,
        members = members,
        newReactions = null,
        pendingReactions = null,
        pending = pending,
        draftMessage = null,
        events = null
    )

    private fun message(
        id: Long,
        tid: Long,
        channelId: Long,
        createdAt: Long,
        deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Sent,
    ) = SceytMessage(
        id = id,
        tid = tid,
        channelId = channelId,
        body = "message $tid",
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
        user = SceytUser(currentUser.id),
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

    private fun attachment(channelId: Long, messageTid: Long) = AttachmentEntity(
        id = 7001,
        messageId = 101,
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

    private fun SceytResponse<SceytChannel>.successData(): SceytChannel {
        assertThat(this).isInstanceOf(SceytResponse.Success::class.java)
        return (this as SceytResponse.Success).data!!
    }
}
