package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.persistence.entity.ChanelMember
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

fun ChanelMember.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user.toUser()
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