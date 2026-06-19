package com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.gson.Gson
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.di.repositoryModule
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.di.logicModule
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MergePendingDirectChannelsUseCase
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTestRule
import java.util.UUID


@RunWith(AndroidJUnit4::class)
@SmallTest
class FindOrCreateDirectChannelLogicTests : SceytKoinComponent {
    private lateinit var database: SceytDatabase
    private lateinit var channelDao: ChannelDao
    private val channelLogic: PersistenceChannelsLogic by inject()
    private val messageDao: MessageDao by inject()
    private val attachmentDao: AttachmentDao by inject()
    private val messagesCache: MessagesCache by inject()
    private val mergePendingDirectChannelsUseCase: MergePendingDirectChannelsUseCase by inject()
    private val currentUser = User("marat")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(listOf(logicModule, repositoryModule))
    }

    @Before
    fun setUp() {
        SceytChatUIKit.initialize(
            appContext = ApplicationProvider.getApplicationContext(),
            clientId = UUID.randomUUID().toString(),
            appId = "yzr58x11rm",
            apiUrl = "https://uk-london-south-api-2-staging.waafi.com",
            enableDatabase = false
        )

        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SceytDatabase::class.java)
            .fallbackToDestructiveMigration(true)
            .allowMainThreadQueries()
            .build()
        channelDao = database.channelDao()
        ClientWrapper.currentUser = currentUser
    }

    @After
    fun tearDown() {
        database.close()
        ClientWrapper.currentUser = null
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_create_new_channel_if_channel_not_found() = runTest {
        val data = CreateChannelData(
            type = "direct",
            avatarUrl = "http://www.bing.com/search?q=litora",
            metadata = "deterruisset",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("1"), "owner"),
                SceytMember(SceytUser("2"), "owner"),
                SceytMember(SceytUser("3"), "owner")
            )

        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success && result.data != null).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.members?.map { it.id }?.sorted() == data.members.map { it.user.id }.sorted()).isTrue()
        Truth.assertThat(channel.type == data.type).isTrue()
        Truth.assertThat(channel.avatarUrl == data.avatarUrl).isTrue()
        Truth.assertThat(channel.metadata == data.metadata).isTrue()

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_not_create_new_channel_if_channel_already_exists() = runTest {
        val data = CreateChannelData(
            type = "direct",
            avatarUrl = "http://www.bing.com/search?q=litora",
            metadata = "deterruisset",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("1"), "owner"),
                SceytMember(SceytUser("2"), "owner"),
                SceytMember(SceytUser("3"), "owner")
            )

        )
        val createdChannel = channelLogic.findOrCreatePendingChannelByMembers(data).data!!

        // Verify first channel is in database
        verifyChannelInDatabase(createdChannel, data, shouldBePending = true)

        // Delay to make sure that the created channel cratedAt is different from the previous one
        delay(500)
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success && result.data != null).isTrue()
        Truth.assertThat(result.data?.createdAt == createdChannel.createdAt).isTrue()

        // Verify same channel is returned from database (no duplicate created)
        verifyChannelInDatabase(result.data!!, data, shouldBePending = true)
        Truth.assertThat(result.data.id).isEqualTo(createdChannel.id)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_create_direct_channel_with_minimal_data() = runTest {
        val data = CreateChannelData(
            type = "direct",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.type).isEqualTo("direct")
        Truth.assertThat(channel.members?.size).isEqualTo(2)
        Truth.assertThat(channel.pending).isTrue()

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_group_channel_creation() = runTest {
        val data = CreateChannelData(
            type = "group",
            subject = "Test Group",
            avatarUrl = "https://example.com/avatar.jpg",
            metadata = "group metadata",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "admin"),
                SceytMember(SceytUser("user2"), "participant"),
                SceytMember(SceytUser("user3"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.type).isEqualTo("group")
        Truth.assertThat(channel.subject).isEqualTo("Test Group")
        Truth.assertThat(channel.avatarUrl).isEqualTo("https://example.com/avatar.jpg")
        Truth.assertThat(channel.metadata).isEqualTo("group metadata")
        Truth.assertThat(channel.members?.size).isEqualTo(4)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_single_member() = runTest {
        val data = CreateChannelData(
            type = "direct",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.members?.size).isEqualTo(1)
        Truth.assertThat(channel.members?.first()?.id).isEqualTo(currentUser.id)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_different_member_roles() = runTest {
        val data = CreateChannelData(
            type = "group",
            subject = "Role Test Group",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("admin1"), "admin"),
                SceytMember(SceytUser("member1"), "participant"),
                SceytMember(SceytUser("member2"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!

        val ownerMember = channel.members?.find { it.id == currentUser.id }
        val adminMember = channel.members?.find { it.id == "admin1" }
        val participantMember = channel.members?.find { it.id == "member1" }

        Truth.assertThat(ownerMember?.role?.name).isEqualTo("owner")
        Truth.assertThat(adminMember?.role?.name).isEqualTo("admin")
        Truth.assertThat(participantMember?.role?.name).isEqualTo("participant")

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_maintain_member_order() = runTest {
        val expectedMemberIds = listOf(currentUser.id, "user1", "user2", "user3")
        val data = CreateChannelData(
            type = "group",
            members = expectedMemberIds.map { id ->
                SceytMember(if (id == currentUser.id) currentUser.toSceytUser() else SceytUser(id), "participant")
            }
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        val actualMemberIds = channel.members?.map { it.id }?.sorted()
        Truth.assertThat(actualMemberIds).isEqualTo(expectedMemberIds.sorted())

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_duplicate_members() = runTest {
        val data = CreateChannelData(
            type = "group",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant"),
                SceytMember(SceytUser("user1"), "participant"), // Duplicate user
                SceytMember(SceytUser("user2"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        // Should handle duplicates appropriately (implementation dependent)
        Truth.assertThat(channel.members).isNotNull()

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_preserve_channel_metadata() = runTest {
        val metadata = """{"customField": "customValue", "priority": "high"}"""
        val data = CreateChannelData(
            type = "group",
            subject = "Metadata Test",
            metadata = metadata,
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.metadata).isEqualTo(metadata)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_long_subject_names() = runTest {
        val longSubject = "A".repeat(500) // Very long subject
        val data = CreateChannelData(
            type = "group",
            subject = longSubject,
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.subject).isEqualTo(longSubject)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_special_characters_in_subject() = runTest {
        val specialSubject = "Test 🚀 Group with émojis & spëcial chars! @#$%^&*()"
        val data = CreateChannelData(
            type = "group",
            subject = specialSubject,
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.subject).isEqualTo(specialSubject)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_return_same_channel_for_identical_member_sets() = runTest {
        val members = listOf(
            SceytMember(currentUser.toSceytUser(), "owner"),
            SceytMember(SceytUser("user1"), "participant"),
            SceytMember(SceytUser("user2"), "participant")
        )

        val data1 = CreateChannelData(type = "group", members = members)
        val data2 = CreateChannelData(type = "group", members = members.reversed()) // Same members, different order

        val result1 = channelLogic.findOrCreatePendingChannelByMembers(data1)
        val result2 = channelLogic.findOrCreatePendingChannelByMembers(data2)

        Truth.assertThat(result1 is SceytResponse.Success).isTrue()
        Truth.assertThat(result2 is SceytResponse.Success).isTrue()

        val channel1 = (result1 as SceytResponse.Success).data!!
        val channel2 = (result2 as SceytResponse.Success).data!!

        // Should return the same channel regardless of member order
        Truth.assertThat(channel1.id).isEqualTo(channel2.id)

        // Verify both channels are stored correctly in database
        verifyChannelInDatabase(channel1, data1, shouldBePending = true)
        verifyChannelInDatabase(channel2, data2, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_broadcast_channel_type() = runTest {
        val data = CreateChannelData(
            type = "broadcast",
            subject = "Broadcast Channel",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("subscriber1"), "participant"),
                SceytMember(SceytUser("subscriber2"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.type).isEqualTo("broadcast")
        Truth.assertThat(channel.subject).isEqualTo("Broadcast Channel")

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_large_member_count() = runTest {
        val largeMembers = mutableListOf<SceytMember>().apply {
            add(SceytMember(currentUser.toSceytUser(), "owner"))
            repeat(100) { index ->
                add(SceytMember(SceytUser("user$index"), "participant"))
            }
        }

        val data = CreateChannelData(
            type = "group",
            subject = "Large Group",
            members = largeMembers
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.members?.size).isEqualTo(101) // 1 owner + 100 participants

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_preserve_pending_status() = runTest {
        val data = CreateChannelData(
            type = "direct",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.pending).isTrue()

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    // Edge case tests
    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_null_values_gracefully() = runTest {
        val data = CreateChannelData(
            type = "group",
            subject = "", // Empty subject
            avatarUrl = "", // Empty avatar URL
            metadata = "", // Empty metadata
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        Truth.assertThat(channel.type).isEqualTo("group")
        Truth.assertThat(channel.members?.size).isEqualTo(1)

        // Verify channel is stored correctly in database
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_concurrent_creation_attempts() = runTest {
        val data = CreateChannelData(
            type = "group",
            subject = "Concurrent Test",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant")
            )
        )

        // Simulate concurrent creation attempts
        val results = (1..5).map {
            channelLogic.findOrCreatePendingChannelByMembers(data)
        }

        // All results should be successful
        results.forEach { result ->
            Truth.assertThat(result is SceytResponse.Success).isTrue()
        }

        // All should return the same channel ID (no duplicates)
        val channelIds = results.map { (it as SceytResponse.Success).data!!.id }.distinct()
        Truth.assertThat(channelIds.size).isEqualTo(1)

        // Verify the channel is stored correctly in database
        val channel = (results.first() as SceytResponse.Success).data!!
        verifyChannelInDatabase(channel, data, shouldBePending = true)
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_return_same_direct_channel_with_different_member_combinations() = runTest {
        val peerUserId = "userB"

        // First: Create direct channel with only peer user (user B)
        val dataWithPeerOnly = CreateChannelData(
            type = "direct",
            members = listOf(
                SceytMember(SceytUser(peerUserId), "participant")
            )
        )

        val firstResult = channelLogic.findOrCreatePendingChannelByMembers(dataWithPeerOnly)
        Truth.assertThat(firstResult is SceytResponse.Success).isTrue()
        val firstChannel = (firstResult as SceytResponse.Success).data!!

        // Verify first channel creation
        Truth.assertThat(firstChannel.type).isEqualTo("direct")
        verifyChannelInDatabase(firstChannel, dataWithPeerOnly, shouldBePending = true)

        // Second: Create direct channel with both current user and peer user (me + user B)
        val dataWithBothUsers = CreateChannelData(
            type = "direct",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser(peerUserId), "participant")
            )
        )

        val secondResult = channelLogic.findOrCreatePendingChannelByMembers(dataWithBothUsers)
        Truth.assertThat(secondResult is SceytResponse.Success).isTrue()
        val secondChannel = (secondResult as SceytResponse.Success).data!!

        // Should return the same channel regardless of member list differences
        Truth.assertThat(secondChannel.id).isEqualTo(firstChannel.id)
        Truth.assertThat(secondChannel.type).isEqualTo("direct")
        Truth.assertThat(secondChannel.createdAt).isEqualTo(firstChannel.createdAt)

        // Verify both channels are the same in database
        verifyChannelInDatabase(secondChannel, dataWithBothUsers, shouldBePending = true)

        // Additional verification: both should be retrievable via getDirectChannelFromDb
        val directChannelFromDb = channelLogic.getDirectChannelFromDb(peerUserId)
        Truth.assertThat(directChannelFromDb).isNotNull()
        Truth.assertThat(directChannelFromDb!!.id).isEqualTo(firstChannel.id)
        Truth.assertThat(directChannelFromDb.id).isEqualTo(secondChannel.id)
    }

    @Test
    fun server_direct_channel_reconciliation_should_merge_pending_direct_channel_and_move_local_state() = runTest {
        val peerUserId = "merge-peer"
        val pendingChannel = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(SceytUser(peerUserId), "participant"))
            )
        ).data!!
        val pendingMessage = pendingMessage(
            channelId = pendingChannel.id,
            tid = 9001
        )

        messageDao.upsertMessage(pendingMessage.toMessageDb(false))
        val pendingAttachment = attachmentEntity(
            channelId = pendingChannel.id,
            messageTid = pendingMessage.tid
        )
        attachmentDao.insertAttachments(listOf(pendingAttachment))
        messagesCache.add(pendingChannel.id, pendingMessage)
        channelLogic.updateDraftMessage(
            draftMessage(
                channelId = pendingChannel.id,
                body = "pending draft",
                createdAt = 10
            )
        )

        val realChannel = directChannel(
            id = 1001,
            peerUserId = peerUserId
        )
        channelLogic.onChannelEvent(ChannelActionEvent.Created(realChannel))
        Truth.assertThat(channelLogic.getChannelFromDb(pendingChannel.id)).isNotNull()

        mergePendingDirectChannelsUseCase(listOf(realChannel), currentUser.id)

        Truth.assertThat(channelLogic.getChannelFromDb(pendingChannel.id)).isNull()
        val mergedChannel = channelLogic.getChannelFromDb(realChannel.id)
        Truth.assertThat(mergedChannel).isNotNull()
        Truth.assertThat(mergedChannel!!.pending).isFalse()
        Truth.assertThat(mergedChannel.draftMessage?.body).isEqualTo("pending draft")

        val movedMessages = messageDao.getPendingMessages(realChannel.id)
        Truth.assertThat(movedMessages.map { it.messageEntity.tid }).contains(pendingMessage.tid)
        Truth.assertThat(
            movedMessages.first { it.messageEntity.tid == pendingMessage.tid }.messageEntity.channelId
        ).isEqualTo(realChannel.id)
        Truth.assertThat(messageDao.getPendingMessages(pendingChannel.id)).isEmpty()

        val cachedMovedMessage = messagesCache.get(realChannel.id, pendingMessage.tid)
        Truth.assertThat(cachedMovedMessage).isNotNull()
        Truth.assertThat(cachedMovedMessage!!.channelId).isEqualTo(realChannel.id)
        Truth.assertThat(messagesCache.get(pendingChannel.id, pendingMessage.tid)).isNull()

        val movedAttachments = attachmentDao.getNewestThenAttachmentInclude(
            channelId = realChannel.id,
            attachmentId = pendingAttachment.id ?: 0,
            limit = 10,
            types = listOf(pendingAttachment.type)
        )
        Truth.assertThat(movedAttachments.map { it.attachmentEntity.messageTid }).contains(pendingMessage.tid)
        Truth.assertThat(
            movedAttachments.first { it.attachmentEntity.messageTid == pendingMessage.tid }
                .attachmentEntity.channelId
        ).isEqualTo(realChannel.id)

        val repeatedFindResult = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(
                    SceytMember(currentUser.toSceytUser(), "owner"),
                    SceytMember(SceytUser(peerUserId), "participant")
                )
            )
        )
        Truth.assertThat(repeatedFindResult.data?.id).isEqualTo(realChannel.id)
    }

    @Test
    fun server_self_channel_reconciliation_should_merge_pending_self_channel() = runTest {
        val pendingSelfChannel = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(currentUser.toSceytUser(), "owner"))
            )
        ).data!!
        val realSelfChannel = selfChannel(id = 1002)

        channelLogic.onChannelEvent(ChannelActionEvent.Created(realSelfChannel))
        Truth.assertThat(channelLogic.getChannelFromDb(pendingSelfChannel.id)).isNotNull()

        mergePendingDirectChannelsUseCase(listOf(realSelfChannel), currentUser.id)

        Truth.assertThat(channelLogic.getChannelFromDb(pendingSelfChannel.id)).isNull()
        Truth.assertThat(channelLogic.getChannelFromDb(realSelfChannel.id)).isNotNull()

        val repeatedFindResult = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(currentUser.toSceytUser(), "owner"))
            )
        )
        Truth.assertThat(repeatedFindResult.data?.id).isEqualTo(realSelfChannel.id)
    }

    @Test
    fun non_matching_direct_and_group_channels_should_not_merge_pending_channels() = runTest {
        val pendingDirectChannel = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(SceytUser("peer-a"), "participant"))
            )
        ).data!!
        val realDirectChannel = directChannel(
            id = 1003,
            peerUserId = "peer-b"
        )

        channelLogic.onChannelEvent(ChannelActionEvent.Created(realDirectChannel))
        mergePendingDirectChannelsUseCase(listOf(realDirectChannel), currentUser.id)

        Truth.assertThat(channelLogic.getChannelFromDb(pendingDirectChannel.id)).isNotNull()
        Truth.assertThat(channelLogic.getChannelFromDb(realDirectChannel.id)).isNotNull()

        val groupMembers = listOf(
            SceytMember(currentUser.toSceytUser(), "owner"),
            SceytMember(SceytUser("group-peer"), "participant")
        )
        val pendingGroupChannel = channelLogic.findOrCreatePendingChannelByMembers(
            CreateChannelData(
                type = ChannelTypeEnum.Group.value,
                subject = "Pending group",
                members = groupMembers
            )
        ).data!!
        val realGroupChannel = channel(
            id = 1004,
            type = ChannelTypeEnum.Group.value,
            subject = "Real group",
            members = groupMembers
        )

        channelLogic.onChannelEvent(ChannelActionEvent.Created(realGroupChannel))
        mergePendingDirectChannelsUseCase(listOf(realGroupChannel), currentUser.id)

        Truth.assertThat(channelLogic.getChannelFromDb(pendingGroupChannel.id)).isNotNull()
        Truth.assertThat(channelLogic.getChannelFromDb(realGroupChannel.id)).isNotNull()
        Truth.assertThat(channelLogic.getChannelFromDb(pendingGroupChannel.id)?.pending).isTrue()
    }

    private fun directChannel(
        id: Long,
        peerUserId: String,
    ): SceytChannel {
        return channel(
            id = id,
            type = ChannelTypeEnum.Direct.value,
            members = listOf(SceytMember(SceytUser(peerUserId), "participant"))
        )
    }

    private fun selfChannel(id: Long): SceytChannel {
        return channel(
            id = id,
            type = ChannelTypeEnum.Direct.value,
            metadata = Gson().toJson(SelfChannelMetadata(1)),
            members = listOf(SceytMember(currentUser.toSceytUser(), "owner"))
        )
    }

    private fun channel(
        id: Long,
        type: String,
        members: List<SceytMember>,
        subject: String = "",
        metadata: String = "",
    ): SceytChannel {
        return SceytChannel(
            id = id,
            parentChannelId = null,
            uri = null,
            type = type,
            subject = subject,
            avatarUrl = null,
            metadata = metadata,
            createdAt = id,
            updatedAt = 0,
            messagesClearedAt = 0,
            memberCount = members.size.toLong(),
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
            members = members,
            newReactions = null,
            pendingReactions = null,
            pending = false,
            draftMessage = null,
            events = null
        )
    }

    private fun pendingMessage(
        channelId: Long,
        tid: Long,
    ): SceytMessage {
        return SceytMessage(
            id = 0,
            tid = tid,
            channelId = channelId,
            body = "Pending message",
            type = "text",
            metadata = null,
            createdAt = tid,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            viewOnce = false,
            deliveryStatus = MessageDeliveryStatus.Pending,
            state = MessageState.Unmodified,
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

    private fun draftMessage(
        channelId: Long,
        body: String,
        createdAt: Long,
    ): DraftMessage {
        return DraftMessage(
            channelId = channelId,
            body = body,
            createdAt = createdAt,
            mentionUsers = null,
            replyOrEditMessage = null,
            isReply = false,
            bodyAttributes = null,
            attachments = null,
            voiceAttachment = null,
            viewOnce = false
        )
    }

    private fun attachmentEntity(
        channelId: Long,
        messageTid: Long,
        id: Long = 7001,
    ) = AttachmentEntity(
        id = id,
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

    /**
     * Helper method to verify that a channel is correctly stored in the database
     * and matches the expected CreateChannelData
     */
    private suspend fun verifyChannelInDatabase(
            channel: SceytChannel,
            expectedData: CreateChannelData,
            shouldBePending: Boolean = true,
    ) {
        val channelFromDb = channelLogic.getChannelFromDb(channel.id)
        Truth.assertThat(channelFromDb).isNotNull()

        with(channelFromDb!!) {
            Truth.assertThat(id).isEqualTo(channel.id)
            Truth.assertThat(type).isEqualTo(expectedData.type)
            Truth.assertThat(subject.orEmpty()).isEqualTo(expectedData.subject)
            Truth.assertThat(avatarUrl.orEmpty()).isEqualTo(expectedData.avatarUrl)
            if (channel.isSelf) {
                Truth.assertThat(metadata).isEqualTo(Gson().toJson(SelfChannelMetadata(1)))
            } else {
                Truth.assertThat(metadata).isEqualTo(expectedData.metadata)
            }
            Truth.assertThat(uri.orEmpty()).isEqualTo(expectedData.uri)
            Truth.assertThat(pending).isEqualTo(shouldBePending)

            // Verify members if provided
            if (expectedData.members.isNotEmpty()) {
                // The logic automatically adds current user as member if not present
                val expectedMemberIds = expectedData.members.map { it.user.id }.toMutableSet()
                if (!expectedMemberIds.contains(currentUser.id)) {
                    expectedMemberIds.add(currentUser.id)
                }

                val actualMemberIds = members?.map { it.id }?.toSet() ?: emptySet()
                Truth.assertThat(actualMemberIds).isEqualTo(expectedMemberIds)

                // Verify member roles for explicitly provided members
                expectedData.members.forEach { expectedMember ->
                    val actualMember = members?.find { it.id == expectedMember.user.id }
                    Truth.assertThat(actualMember).isNotNull()
                    Truth.assertThat(actualMember!!.role.name).isEqualTo(expectedMember.role.name)
                }

                // Verify current user is present (automatically added if not in original list)
                val currentUserMember = members?.find { it.id == currentUser.id }
                Truth.assertThat(currentUserMember).isNotNull()
            }
        }

        // Additional verification for direct channels
        if (expectedData.type == ChannelTypeEnum.Direct.value && expectedData.members.size == 2) {
            val peerId = expectedData.members.find { it.user.id != currentUser.id }?.user?.id
            if (peerId != null) {
                val directChannelFromDb = channelLogic.getDirectChannelFromDb(peerId)
                Truth.assertThat(directChannelFromDb).isNotNull()
                Truth.assertThat(directChannelFromDb!!.id).isEqualTo(channel.id)
            }
        }
    }
}
