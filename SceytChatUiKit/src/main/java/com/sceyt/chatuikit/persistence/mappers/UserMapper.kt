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
import com.sceyt.chatuikit.persistence.entity.channel.ChanelMemberDb
import com.sceyt.chatuikit.persistence.entity.user.UserDb
import com.sceyt.chatuikit.persistence.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.entity.user.UserMetadataEntity

fun ChanelMemberDb.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user?.toSceytUser() ?: SceytUser(link.userId)
)

fun UserDb.toSceytUser() = with(user) {
    SceytUser(
        id = id,
        username = username,
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        avatarURL = avatarURL,
        metadataMap = metadata.associate { it.key to it.value },
        presence = presence,
        state = activityStatus ?: UserState.Active,
        blocked = blocked)
}


fun UserDb.toUser() = with(user) {
    User(
        id, username, firstName, lastName, avatarURL, metadata.associate { it.key to it.value },
        presence, activityStatus, blocked
    )
}

fun SceytMember.toUserDb() = UserDb(
    user = user.toUserEntity(),
    metadata = user.metadataMap?.map { (key, value) -> UserMetadataEntity(id, key, value) }.orEmpty()
)

fun SceytUser.toUserEntity(): UserEntity {
    return UserEntity(
        id, username, firstName, lastName, avatarURL, presence, state, blocked
    )
}

fun User.toUserDb() = UserDb(
    user = UserEntity(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        avatarURL = avatarURL,
        presence = presence,
        activityStatus = activityState,
        blocked = blocked
    ),
    metadata = metadataMap?.map { (key, value) -> UserMetadataEntity(id, key, value) }.orEmpty()
)

fun SceytUser.toUserDb() = UserDb(
    user = UserEntity(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        avatarURL = avatarURL,
        presence = presence,
        activityStatus = state,
        blocked = blocked),
    metadata = metadataMap?.map { (key, value) -> UserMetadataEntity(id, key, value) }.orEmpty()
)

fun User.toSceytUser() = SceytUser(
    id = id,
    username = username.orEmpty(),
    firstName = firstName,
    lastName = lastName,
    avatarURL = avatarURL,
    metadataMap = metadataMap,
    presence = presence,
    state = activityState,
    blocked = blocked
)

fun SceytUser.toUser() = User(
    id, username, firstName, lastName, avatarURL, metadataMap, presence, state, blocked
)

fun Member.MemberType.toRoleType(): RoleTypeEnum {
    return when (this) {
        Member.MemberType.MemberTypeNone -> RoleTypeEnum.None
        Member.MemberType.MemberTypeOwner -> RoleTypeEnum.Owner
        Member.MemberType.MemberTypeMember -> RoleTypeEnum.Member
    }
}

fun SceytUser.isDeleted() = state == UserState.Deleted

fun createEmptyUser(id: String, displayName: String): SceytUser {
    return SceytUser(id, "", displayName, "", "", null,
        Presence(PresenceState.Offline, "", 0), UserState.Active, false)
}