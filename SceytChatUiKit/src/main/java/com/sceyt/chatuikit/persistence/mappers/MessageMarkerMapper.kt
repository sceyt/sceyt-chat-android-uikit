package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerWithUserDb

internal fun MarkerEntity.toSceytMarker(user: SceytUser) = SceytMarker(messageId, user, name, createdAt)

internal fun MarkerWithUserDb.toMarker(): SceytMarker {
    return with(entity) {
        val user = user?.toSceytUser() ?: SceytUser(userId)
        SceytMarker(messageId, user, name, createdAt)
    }
}

internal fun SceytMarker.toMarkerEntity() = MarkerEntity(messageId, userId, name, createdAt)

fun Marker.toSceytMarker() = SceytMarker(messageId, user.toSceytUser(), name, createdAt)

fun SceytMarker.toMarker() = Marker(messageId, user?.toUser() ?: User(userId), name, createdAt)

