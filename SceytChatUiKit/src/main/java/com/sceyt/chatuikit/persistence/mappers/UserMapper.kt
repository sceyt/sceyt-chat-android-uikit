package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.entity.UserEntity
import com.sceyt.chatuikit.persistence.entity.channel.ChanelMemberDb

fun ChanelMemberDb.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user?.toSceytUser() ?: SceytUser(link.userId)
)

fun UserEntity.toSceytUser() = SceytUser(
    id = id,
    firstName = firstName.orEmpty(),
    lastName = lastName.orEmpty(),
    avatarURL = avatarURL,
    metadata = metadata,
    presence = presence,
    state = activityStatus ?: UserState.Active,
    blocked = blocked
)

fun UserEntity.toUser() = User(
    id, firstName, lastName, avatarURL, metadata, presence, activityStatus, blocked
)

fun SceytMember.toUserEntity(): UserEntity {
    with(user) {
        return UserEntity(
            id, firstName, lastName, avatarURL, metadata, presence, state, blocked
        )
    }
}

fun User.toUserEntity(): UserEntity {
    return UserEntity(
        id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
    )
}

fun SceytUser.toUserEntity(): UserEntity {
    return UserEntity(
        id, firstName, lastName, avatarURL, metadata, presence, state, blocked
    )
}

fun User.toSceytUser() = SceytUser(
    id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
)

fun SceytUser.toUser() = User(
    id, firstName, lastName, avatarURL, metadata, presence, state, blocked
)

fun Member.toUserEntity(): UserEntity {
    return UserEntity(
        id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
    )
}

fun Member.MemberType.toRoleType(): RoleTypeEnum {
    return when (this) {
        Member.MemberType.MemberTypeNone -> RoleTypeEnum.None
        Member.MemberType.MemberTypeOwner -> RoleTypeEnum.Owner
        Member.MemberType.MemberTypeMember -> RoleTypeEnum.Member
    }
}

fun SceytUser.isDeleted() = state == UserState.Deleted

fun createEmptyUser(id: String, displayName: String): SceytUser {
    return SceytUser(id, displayName, "", "", "",
        Presence(PresenceState.Offline, "", 0), UserState.Active, false)
}