package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionTotalEntity

fun Reaction.toReactionEntity() = ReactionEntity(
    id = id,
    messageId = messageId,
    key = key,
    score = score,
    reason = reason,
    updatedAt = createdAt.time,
    fromId = user.id
)

fun Reaction.toReactionDb() = ReactionDb(
    reaction = toReactionEntity(),
    from = user.toUserEntity()
)

fun ReactionTotal.toReactionTotalEntity(messageId: Long) = ReactionTotalEntity(
    messageId = messageId,
    key = key,
    count = count,
    score = score.toInt()
)

fun ReactionDb.toReaction(): Reaction {
    with(reaction) {
        return Reaction(id, messageId, key, score, reason, updatedAt, from?.toUser())
    }
}

fun MarkerEntity.toMarker(user: User) = Marker(messageId, user, name, createdAt)

fun ChatUserReactionDb.toReaction(): Reaction {
    with(reaction) {
        return Reaction(id, messageId, key, score, reason, createdAt, from?.toUser())
    }
}

fun Reaction.toUserReactionsEntity(channelId: Long) = ChatUserReactionEntity(
    id = id,
    messageId = messageId,
    channelId = channelId,
    key = key,
    score = score,
    reason = reason,
    createdAt = createdAt.time,
    fromId = user.id
)

fun ReactionTotalEntity.toReactionTotal(): ReactionTotal = ReactionTotal(key, count, score.toLong())
