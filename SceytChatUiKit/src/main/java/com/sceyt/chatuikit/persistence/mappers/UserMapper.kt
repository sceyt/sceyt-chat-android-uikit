package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.entity.channel.ChanelMemberDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserMetadataEntity

internal fun ChanelMemberDb.toSceytMember() = SceytMember(
    role = Role(link.role),
    user = user?.toSceytUser() ?: SceytUser(link.userId)
)

internal fun UserDb.toSceytUser() = with(user) {
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


internal fun UserDb.toUser() = with(user) {
    User(
        id, username, firstName, lastName, avatarURL, metadata.associate { it.key to it.value },
        presence, activityStatus, blocked
    )
}

internal fun SceytMember.toUserDb() = UserDb(
    user = user.toUserEntity(),
    metadata = user.metadataMap?.map { (key, value) -> UserMetadataEntity(id, key, value) }.orEmpty()
)

internal fun SceytUser.toUserEntity(): UserEntity {
    return UserEntity(
        id, username, firstName, lastName, avatarURL, presence, state, blocked
    )
}

internal fun User.toUserDb() = UserDb(
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

internal fun SceytUser.toUserDb() = UserDb(
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

fun SceytUser.isDeleted() = state == UserState.Deleted

fun createEmptyUser(id: String, displayName: String = ""): SceytUser {
    return SceytUser(id, "", displayName, "", "", null,
        Presence(PresenceState.Offline, "", 0), UserState.Active, false)
}