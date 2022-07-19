package com.sceyt.chat.ui.persistence.mappers

import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum.*
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.persistence.entity.MemberEntity
import com.sceyt.chat.ui.persistence.entity.UserEntity
import com.sceyt.chat.ui.persistence.entity.channel.ChannelDb
import com.sceyt.chat.ui.persistence.entity.channel.ChannelEntity
import java.util.*

fun SceytChannel.toChannelEntity(): ChannelEntity {
    var memberCount = 1L
    if (isGroup)
        memberCount = (this as SceytGroupChannel).memberCount

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
        muteUntil = muteUntil,
        subject = channelSubject,
        avatarUrl = getChannelAvatarUrl(),
        memberCount = memberCount
    )
}

fun ChannelDb.toChannel(): SceytChannel {
    with(channelEntity) {
        return when (type) {
            Private, Public ->
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
                    channelType = type,
                    subject = subject,
                    avatarUrl = avatarUrl,
                    members = members.map { it.toSceytMember() },
                    memberCount = memberCount,
                )
            Direct -> SceytDirectChannel(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                unreadMessageCount = unreadMessageCount,
                lastMessage = lastMessage?.toSceytMessage(),
                label = label,
                metadata = metadata,
                muted = muted,
                muteUntil = 0,
                peer = members[0].toSceytMember())
        }
    }
}

fun MemberEntity.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user.toUser()
)

fun UserEntity.toUser() = User(
    id, firstName, lastName, avatarURL, metadata, presence, activityStatus, blocked
)

fun SceytMember.toMemberEntity(): UserEntity {
    with(user) {
        return UserEntity(
            id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
        )
    }
}