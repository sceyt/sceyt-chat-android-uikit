package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.config.SystemMessagesConfig
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.DisappearingMessageMetadata
import com.sceyt.chatuikit.data.models.messages.MembersMetaData
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.SystemMessageAction
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class SystemMessageSenderImplTest {
    private val messagesLogic = mock<PersistenceMessagesLogic>()
    private val sender = SystemMessageSenderImpl(messagesLogic)
    private val gson = Gson()
    private lateinit var clientWrapperMock: MockedStatic<ClientWrapper>

    @Before
    fun setUp() {
        SceytChatUIKit.config = SceytChatUIKitConfig()
        clientWrapperMock = Mockito.mockStatic(ClientWrapper::class.java)
        clientWrapperMock.`when`<Long> { ClientWrapper.generateTid() }.thenReturn(MESSAGE_TID)
    }

    @After
    fun tearDown() {
        clientWrapperMock.close()
    }

    @Test
    fun sendGroupCreated_buildsSilentSystemMessage() = runTest {
        sender.sendGroupCreated(CHANNEL_ID)

        val message = sentMessage()

        assertSystemMessage(message, SystemMessageAction.GroupCreated)
        assertThat(message.tid).isEqualTo(MESSAGE_TID)
        assertThat(message.metadata.orEmpty()).isEmpty()
        assertThat(message.mentionedUsers?.toList().orEmpty()).isEmpty()
    }

    @Test
    fun sendMembersAdded_buildsMemberMetadataAndMentionedUsers() = runTest {
        sender.sendMembersAdded(
            channelId = CHANNEL_ID,
            members = listOf(member("first"), member("second"))
        )

        val message = sentMessage()

        assertSystemMessage(
            message = message,
            body = SystemMessageAction.MemberAdded,
            disableMentionsCount = true
        )
        assertThat(message.tid).isEqualTo(MESSAGE_TID)
        assertThat(memberIds(message.metadata)).containsExactly("first", "second").inOrder()
        assertThat(message.mentionedUsers?.map { it.id }).containsExactly("first", "second").inOrder()
    }

    @Test
    fun sendMembersRemoved_withEmptyMembersDoesNotSendMessage() = runTest {
        sender.sendMembersRemoved(CHANNEL_ID, emptyList())

        verifyNoInteractions(messagesLogic)
    }

    @Test
    fun sendMemberLeft_whenDisabledDoesNotSendMessage() = runTest {
        SceytChatUIKit.config.systemMessagesConfig = SystemMessagesConfig(memberLeft = false)

        sender.sendMemberLeft(CHANNEL_ID)

        verifyNoInteractions(messagesLogic)
    }

    @Test
    fun sendDisappearingMessageChanged_buildsDurationMetadata() = runTest {
        sender.sendDisappearingMessageChanged(CHANNEL_ID, duration = 60_000L)

        val message = sentMessage()

        assertSystemMessage(message, SystemMessageAction.DisappearingMessage)
        assertThat(message.tid).isEqualTo(MESSAGE_TID)
        val metadata = gson.fromJson(message.metadata, DisappearingMessageMetadata::class.java)
        assertThat(metadata.duration).isEqualTo("60000")
    }

    private suspend fun sentMessage(): Message {
        val captor = argumentCaptor<Message>()
        verify(messagesLogic).sendMessage(eq(CHANNEL_ID), captor.capture())
        return captor.firstValue
    }

    private fun assertSystemMessage(
        message: Message,
        body: SystemMessageAction,
        disableMentionsCount: Boolean = false,
    ) {
        assertThat(message.type).isEqualTo(SceytMessageType.System.value)
        assertThat(message.body).isEqualTo(body.value)
        assertThat(message.silent).isTrue()
        assertThat(message.displayCount.toInt()).isEqualTo(0)
        assertThat(message.disableMentionsCount).isEqualTo(disableMentionsCount)
    }

    private fun memberIds(metadata: String?): List<String>? {
        return gson.fromJson(metadata, MembersMetaData::class.java).members
    }

    private fun member(id: String): SceytMember {
        return SceytMember(Role("member"), SceytUser(id))
    }

    private companion object {
        const val CHANNEL_ID = 123L
        const val MESSAGE_TID = 456L
    }
}
