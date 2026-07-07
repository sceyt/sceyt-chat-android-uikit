package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.DisappearingMessageMetadata
import com.sceyt.chatuikit.data.models.messages.MembersMetaData
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SystemMessageAction
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.mappers.toUser

class SystemMessageSenderImpl(
    private val messagesLogic: PersistenceMessagesLogic
) : SystemMessageSender {
    private val gson = Gson()

    override suspend fun sendGroupCreated(channelId: Long) {
        sendSystemMessage(channelId, SystemMessageAction.GroupCreated)
    }

    override suspend fun sendChannelCreated(channelId: Long) {
        sendSystemMessage(channelId, SystemMessageAction.ChannelCreated)
    }

    override suspend fun sendMembersAdded(channelId: Long, members: List<SceytMember>) {
        sendMembersMessage(channelId, members, SystemMessageAction.MemberAdded)
    }

    override suspend fun sendMembersRemoved(channelId: Long, members: List<SceytMember>) {
        sendMembersMessage(channelId, members, SystemMessageAction.MemberRemoved)
    }

    override suspend fun sendMemberLeft(channelId: Long) {
        sendSystemMessage(channelId, SystemMessageAction.MemberLeaved)
    }

    override suspend fun sendJoinedByInviteLink(channelId: Long) {
        sendSystemMessage(channelId, SystemMessageAction.JoinByInviteLink)
    }

    override suspend fun sendDisappearingMessageChanged(channelId: Long, duration: Long) {
        sendSystemMessage(
            channelId = channelId,
            body = SystemMessageAction.DisappearingMessage,
            metadata = gson.toJson(DisappearingMessageMetadata(duration.toString()))
        )
    }

    private suspend fun sendMembersMessage(
        channelId: Long,
        members: List<SceytMember>,
        body: SystemMessageAction,
    ) {
        if (members.isEmpty()) return

        sendSystemMessage(
            channelId = channelId,
            body = body,
            metadata = gson.toJson(MembersMetaData(members.map { it.id })),
            mentionedUsers = members.map { it.user.toUser() },
            disableMentionsCount = true
        )
    }

    private suspend fun sendSystemMessage(
        channelId: Long,
        body: SystemMessageAction,
        metadata: String? = null,
        mentionedUsers: List<User>? = null,
        disableMentionsCount: Boolean = false,
    ) {
        if (!SceytChatUIKit.config.systemMessagesConfig.isEnabled(body)) return

        val builder = Message.MessageBuilder()
            .setType(SceytMessageType.System.value)
            .withDisplayCount(0)
            .setSilent(true)
            .setBody(body.value)

        metadata?.let(builder::setMetadata)
        mentionedUsers?.takeIf { it.isNotEmpty() }?.let {
            builder.setMentionedUsers(it.toTypedArray())
        }
        if (disableMentionsCount) {
            builder.setDisableMentionsCount(true)
        }

        messagesLogic.sendMessage(channelId, Message(builder))
    }
}
