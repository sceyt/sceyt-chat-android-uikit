package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.entity.messages.MarkerWithUserDb

fun MarkerEntity.toSceytMarker(user: User) = SceytMarker(messageId, user, name, createdAt)

fun MarkerWithUserDb.toMarker(): SceytMarker {
    return with(entity) {
        val user = user?.toUser() ?: User(userId)
        SceytMarker(messageId, user, name, createdAt)
    }
}

fun SceytMarker.toMarkerEntity() = MarkerEntity(messageId, userId, name, createdAt)

fun Marker.toSceytMarker() = SceytMarker(messageId, user, name, createdAt)

fun SceytMarker.toMarker() = Marker(messageId, user ?: User(userId), name, createdAt)

