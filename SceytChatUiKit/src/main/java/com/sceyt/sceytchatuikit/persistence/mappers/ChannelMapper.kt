package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import java.util.*

fun SceytChannel.toChannelEntity(currentUserId: String?): ChannelEntity {
    var memberCount = 1L
    var myRole: RoleTypeEnum? = null
    if (isGroup) {
        memberCount = (this as SceytGroupChannel).memberCount
        myRole = getMyRoleType(currentUserId)
    }

    return ChannelEntity(
        id = id,
        type = channelType,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadMessageCount = unreadMessageCount,
        lastMessageId = lastMessage?.id,
        lastMessageAt = lastMessage?.createdAt,
        label = label,
        metadata = metadata,
        muted = muted,
        muteExpireDate = muteExpireDate?.time,
        markedUsUnread = markedUsUnread,
        subject = if (isGroup) channelSubject else null,
        avatarUrl = getChannelAvatarUrl(),
        memberCount = memberCount,
        myRole = myRole
    )
}

fun Channel.toChannelEntity(): ChannelEntity {
    var memberCount = 1L
    var subject = ""
    val avatarUrl: String
    var myRole: Member.MemberType? = null

    if (this is GroupChannel) {
        memberCount = this.memberCount
        subject = this.subject
        avatarUrl = this.avatarUrl
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
        lastMessageId = lastMessage?.id,
        lastMessageAt = lastMessage?.createdAt?.time,
        label = label,
        metadata = metadata,
        muted = muted(),
        muteExpireDate = muteExpireDate()?.time,
        markedUsUnread = false,/////Todo
        subject = subject,
        avatarUrl = avatarUrl,
        memberCount = memberCount,
        myRole = myRole?.toRoleType()
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
                peer = members?.firstOrNull()?.toSceytMember())
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