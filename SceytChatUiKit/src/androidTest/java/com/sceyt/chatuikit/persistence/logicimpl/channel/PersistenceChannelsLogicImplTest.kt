package com.sceyt.chatuikit.persistence.logicimpl.channel

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.CreateChannelRequest
import com.sceyt.chat.models.channel.DeleteChannelRequest
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ChannelCallback
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager.awaitToConnectSceytWithTimeout
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.extensions.toSha256
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.coroutines.resume

class PersistenceChannelsLogicImplTest {


    @Before
    fun setUp() {
        SceytChatUIKit.initialize(
            appContext = ApplicationProvider.getApplicationContext(),
            clientId = UUID.randomUUID().toString(),
            appId = "yzr58x11rm",
            apiUrl = "https://uk-london-south-api-2-staging.waafi.com",
            enableDatabase = false
        )
    }

    @Test
    fun createDirectChannel() = runTest {
        ChatClient.getClient().connect("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE3MDk3MDYzMTUsImV4cCI6MTcwOTc5MjcxNSwibmJmIjoxNzA5NzA2MzE1LCJzdWIiOiJtIn0.XKjoe0haR-WdNaHZ7OSXMlTxTf0Lu2zhoXboLE3eeIcf3mFynpjupoX2ZeJd-2wkGOefbespHYvRhS9jV5vxAtk0LnuWCoYVWWKTrqwJBqfTDePdQaceyViYPqoR8PUYHhs1vRxBKhik7BK2cs39HJ_Z8Ck1z9lTFBO08xY3i_Ygr-cNDc0ZZGgbFrwf4L8NQjCtr3Mg2w2dFatl7o9K2WTi7mKTBwnTjhqwsIFNr_JUNDpoz4WfgJokhXx5CwzSj94xH1i3h4yon7PE8ZwsabApjbVhHiSTezL1yF71x60pIhS-j-xxlesKIa9a7ATfCSKlXM4ZjhZWlfBgxdRTyw")
        awaitToConnectSceytWithTimeout(5000L)

        val createdBy = ClientWrapper.currentUser

        val role = Role(RoleTypeEnum.Owner.value)
        val members = setOf(SceytMember(role, SceytUser("1234")), SceytMember(role, createdBy.toSceytUser())).toList()
        val channelId = members.map { it.id }.toSet().sorted().joinToString(separator = "$").toSha256()

        val createdChannel = suspendCancellableCoroutine {
            initCreateChannelRequest(CreateChannelData(
                channelType = ChannelTypeEnum.Direct.value,
                members = members,
                uri = "",
                metadata = "",
            )).execute(object : ChannelCallback {
                override fun onResult(p0: Channel?) {
                    it.resume(p0)
                }

                override fun onError(p0: SceytException?) {
                    it.resume(null)
                }
            })
        }

        Log.i("CreateDirectChannel", "Channel created successfully: ${createdChannel?.id} local channelId : $channelId")

        suspendCancellableCoroutine { cont ->
            createdChannel?.id?.let {
                DeleteChannelRequest(it).execute(object : ActionCallback {
                    override fun onSuccess() {
                        Log.i("CreateDirectChannel", "Channel deleted successfully")
                        cont.resume(Unit)
                    }

                    override fun onError(p0: SceytException?) {
                        Log.e("CreateDirectChannel", "Channel deleted error: ${p0?.message}")
                        cont.resume(Unit)
                    }
                })
            }
        }

        assertTrue(createdChannel != null)
    }


    private fun initCreateChannelRequest(channelData: CreateChannelData): CreateChannelRequest {
        return CreateChannelRequest.Builder(channelData.channelType)
            .withMembers(channelData.members.map { it.toMember() })
            .withUri(channelData.uri)
            .withAvatarUrl(channelData.avatarUrl)
            .withSubject(channelData.subject)
            .withMetadata(channelData.metadata)
            .build()
    }
}