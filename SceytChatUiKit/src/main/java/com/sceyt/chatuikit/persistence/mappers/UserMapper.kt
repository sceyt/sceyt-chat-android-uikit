package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.persistence.entity.UserEntity
import com.sceyt.chatuikit.persistence.entity.channel.ChanelMemberDb

fun ChanelMemberDb.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user?.toUser() ?: User(link.userId)
)

fun UserEntity.toUser() = User(
    id, firstName, lastName, avatarURL, metadata, presence, activityStatus, blocked
)

fun SceytMember.toUserEntity(): UserEntity {
    with(user) {
        return UserEntity(
            id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
        )
    }
}

fun User.toUserEntity(): UserEntity {
    return UserEntity(
        id, firstName, lastName, avatarURL, metadata, presence, activityState, blocked
    )
}

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

fun User.isDeleted() = activityState == UserState.Deleted

fun createEmptyUser(id: String, displayName: String): User {
    return User(id, displayName, "", "", "",
        Presence(PresenceState.Offline, "", 0), UserState.Active, false)
}