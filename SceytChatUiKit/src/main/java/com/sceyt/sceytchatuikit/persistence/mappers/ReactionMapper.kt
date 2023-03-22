package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionScoreEntity

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

fun ReactionScore.toReactionScoreEntity(messageId: Long) = ReactionScoreEntity(
    messageId = messageId,
    key = key,
    score = score.toInt()
)

fun ReactionDb.toReaction(): Reaction {
    with(reaction) {
        return Reaction(id, messageId, key, score, reason, updatedAt, from?.toUser())
    }
}


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

fun ReactionScoreEntity.toReactionScore(): ReactionScore = ReactionScore(key, score.toLong())
