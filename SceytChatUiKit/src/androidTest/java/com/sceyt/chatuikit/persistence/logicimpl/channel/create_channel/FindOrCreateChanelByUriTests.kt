package com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel

import androidx.annotation.VisibleForTesting
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.di.repositoryModule
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager.awaitToConnectSceytWithTimeout
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
class FindOrCreateChanelByUriTests : SceytKoinComponent {
    private lateinit var database: SceytDatabase
    private lateinit var channelDao: ChannelDao
    private val channelLogic: PersistenceChannelsLogic by inject()
    private val currentUser = User("marat")
    private val token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE3NTg2MTQ5NTQsImV4cCI6MTc1ODcwMTM1NCwibmJmIjoxNzU4NjE0OTU0LCJzdWIiOiJtYXJhdCJ9.VZz6jbK_VR1WaAgwgRhoERdjtZ1FxbZaQEvM32YVa9UF_Ehg-AV5kyv4Y4tAgNQXzU_Eq8Tj3iIBPVzkj60mKpNzCGlY0laZSopkotKFyK6n8uwVvYzKV1dNEYr0Lm8mHJcCVox8KSAWZI2RSMkGmZbqwsVuMMse-FwNNALCzD_XUuRr2FnkOQLjiImj_dbqnQzcSw8nggakCHvfhuJmRfqht3uomxLns8mWqAve_EN-piON7ub2msD56caMUoYbQynZ2q_2DlfvHKMVnwYWHdhx1iEKiB2s7sVCKu4X_91w9SVBwauFh8c00sBZbvCYCJ-NVNRVrfM_xlfW2urUOw"

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
    fun findOrCreatePendingChannelByUri_should_create_new_channel_if_channel_not_found() = runTest {
        ChatClient.getClient().connect(token)
        awaitToConnectSceytWithTimeout(5000L)

        val uri = System.currentTimeMillis().toString()
        val response = channelLogic.getChannelFromServerByUri(uri)
        if (!response.assertIfError("Error getting channel by uri")) {
            if (response.data == null) {
                val data = CreateChannelData(
                    type = "direct",
                    avatarUrl = "http://www.bing.com/search?q=litora",
                    metadata = "deterruisset",
                    members = listOf(
                        SceytMember(currentUser.toSceytUser(), "owner"),
                        SceytMember(SceytUser("1"), "owner"),
                        SceytMember(SceytUser("2"), "owner"),
                        SceytMember(SceytUser("3"), "owner")
                    ),
                    uri = uri
                )
                when (val result = channelLogic.findOrCreatePendingChannelByUri(data)) {
                    is SceytResponse.Error -> Truth.assertWithMessage("Error creating channel: ${result.message}").fail()
                    is SceytResponse.Success -> {
                        val channel = result.data
                        if (channel == null) {
                            Truth.assertWithMessage("Channel not created").fail()
                        } else {
                            Truth.assertThat(channel.members?.map { it.id }?.sorted() == data.members.map { it.user.id }.sorted()).isTrue()
                            Truth.assertThat(channel.type == data.type).isTrue()
                            Truth.assertThat(channel.avatarUrl == data.avatarUrl).isTrue()
                            Truth.assertThat(channel.metadata == data.metadata).isTrue()
                            Truth.assertThat(channel.uri == data.uri).isTrue()
                        }
                    }
                }
            } else {
                Truth.assertWithMessage("Channel already exists").fail()
            }
        }
    }

    @Test
    fun findOrCreatePendingChannelByUri_should_not_create_new_channel_if_channel_already_exists() = runTest {
        ChatClient.getClient().connect(token)
        awaitToConnectSceytWithTimeout(5000L)
        val uri = System.currentTimeMillis().toString()
        val response = channelLogic.getChannelFromServerByUri(uri)
        if (response is SceytResponse.Success && response.data == null) {
            val data = CreateChannelData(
                type = "direct",
                avatarUrl = "http://www.bing.com/search?q=litora",
                metadata = "deterruisset",
                members = listOf(
                    SceytMember(currentUser.toSceytUser(), "owner"),
                    SceytMember(SceytUser("1"), "owner"),
                    SceytMember(SceytUser("2"), "owner"),
                    SceytMember(SceytUser("3"), "owner")
                ),
                uri = uri
            )
            val createdChannel = channelLogic.findOrCreatePendingChannelByUri(data).data!!
            // Delay to make sure that the created channel cratedAt is different from the previous one
            delay(500)
            val result = channelLogic.findOrCreatePendingChannelByUri(data)

            if (!result.assertIfError("Error creating channel")) {
                Truth.assertThat(result.data?.createdAt == createdChannel.createdAt).isTrue()
            }
        }
    }
}

@VisibleForTesting
fun SceytResponse<*>.assertIfError(message: String = ""): Boolean {
    return if (this is SceytResponse.Error) {
        Truth.assertWithMessage("$message:exception ${this.message}").fail()
        true
    } else {
        false
    }
}