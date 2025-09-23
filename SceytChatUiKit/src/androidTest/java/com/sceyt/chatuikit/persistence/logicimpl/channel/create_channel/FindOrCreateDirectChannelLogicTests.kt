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
}