package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.sceytchatuikit.data.getChannelUrl
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import java.util.*

fun SceytChannel.toChannelEntity(currentUserId: String?): ChannelEntity {
    var memberCount = 1L
    var myRole: RoleTypeEnum? = null
    var channelUrl: String? = null
    if (isGroup) {
        memberCount = (this as SceytGroupChannel).memberCount
        myRole = getMyRoleType(currentUserId)
        channelUrl = this.channelUrl
    }

    return ChannelEntity(
        id = id,
        type = channelType,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadMessageCount = unreadMessageCount,
        lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
        lastMessageAt = lastMessage?.createdAt,
        label = label,
        metadata = metadata,
        muted = muted,
        muteExpireDate = muteExpireDate?.time,
        markedUsUnread = markedUsUnread,
        subject = if (isGroup) channelSubject else null,
        avatarUrl = getChannelAvatarUrl(),
        memberCount = memberCount,
        myRole = myRole,
        channelUrl = channelUrl
    )
}

private fun getTid(msgId: Long?, tid: Long?, incoming: Boolean?): Long? {
    return if (incoming == true)
        msgId
    else tid
}

fun Channel.toChannelEntity(): ChannelEntity {
    var memberCount = 1L
    var subject = ""
    val avatarUrl: String
    var channelUrl: String? = null
    var myRole: Member.MemberType? = null

    if (this is GroupChannel) {
        memberCount = this.memberCount
        subject = this.subject
        avatarUrl = this.avatarUrl
        channelUrl = this.getChannelUrl()
        myRole = myRole()
    } else {
        this as DirectChannel
        avatarUrl = this.peer.avatarURL
    }

    return ChannelEntity(
        id = id,
        type = getChannelType(this),
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadMessageCount = unreadMessageCount,
        lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
        lastMessageAt = lastMessage?.createdAt?.time,
        label = label,
        metadata = metadata,
        muted = muted(),
        muteExpireDate = muteExpireDate()?.time,
        markedUsUnread = markedAsUnread(),
        subject = subject,
        avatarUrl = avatarUrl,
        memberCount = memberCount,
        myRole = myRole?.toRoleType(),
        channelUrl = channelUrl
    )
}

fun ChannelDb.toChannel(): SceytChannel {
    with(channelEntity) {
        return when (type) {
            ChannelTypeEnum.Private, ChannelTypeEnum.Public ->
                SceytGroupChannel(
                    id = id,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    unreadMessageCount = unreadMessageCount,
                    lastMessage = lastMessage?.toSceytMessage(),
                    label = label,
                    metadata = metadata,
                    muted = muted,
                    muteExpireDate = Date(muteExpireDate ?: 0),
                    markedUsUnread = markedUsUnread,
                    channelType = type,
                    subject = subject,
                    avatarUrl = avatarUrl,
                    channelUrl = channelUrl,
                    members = members?.map { it.toSceytMember() } ?: arrayListOf(),
                    memberCount = memberCount,
                )
            ChannelTypeEnum.Direct -> SceytDirectChannel(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                unreadMessageCount = unreadMessageCount,
                lastMessage = lastMessage?.toSceytMessage(),
                label = label,
                metadata = metadata,
                muted = muted,
                peer = members?.firstOrNull()?.toSceytMember(),
                markedUsUnread = markedUsUnread
            )
        }
    }
}


fun SceytGroupChannel.getMyRoleType(currentUserId: String?): RoleTypeEnum {
    return members.find { it.id == currentUserId }?.let {
        if (it.role.name == "owner")
            RoleTypeEnum.Owner
        else RoleTypeEnum.Member
    } ?: run { RoleTypeEnum.None }
}