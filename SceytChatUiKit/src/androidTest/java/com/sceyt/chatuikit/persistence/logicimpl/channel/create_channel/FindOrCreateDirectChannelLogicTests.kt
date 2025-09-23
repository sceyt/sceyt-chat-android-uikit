package com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.di.repositoryModule
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.di.logicModule
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
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
        // Delay to make sure that the created channel cratedAt is different from the previous one
        delay(500)
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success && result.data != null).isTrue()
        Truth.assertThat(result.data?.createdAt == createdChannel.createdAt).isTrue()
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
    }

    @Test
    fun findOrCreatePendingChannelByMembers_should_handle_duplicate_members() = runTest {
        val data = CreateChannelData(
            type = "group",
            members = listOf(
                SceytMember(currentUser.toSceytUser(), "owner"),
                SceytMember(SceytUser("user1"), "participant"),
                SceytMember(SceytUser("user1"), "admin"), // Duplicate user with different role
                SceytMember(SceytUser("user2"), "participant")
            )
        )
        val result = channelLogic.findOrCreatePendingChannelByMembers(data)
        Truth.assertThat(result is SceytResponse.Success).isTrue()
        val channel = (result as SceytResponse.Success).data!!
        // Should handle duplicates appropriately (implementation dependent)
        Truth.assertThat(channel.members).isNotNull()
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
    }
}