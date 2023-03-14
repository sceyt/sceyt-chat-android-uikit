package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionScoreEntity

fun Reaction.toReactionEntity(messageId: Long) = ReactionEntity(
    id = id,
    messageId = messageId,
    key = key,
    score = score,
    reason = reason,
    updateAt = updateAt.time,
    fromId = user.id
)

fun Reaction.toReactionDb(messageId: Long) = ReactionDb(
    reaction = toReactionEntity(messageId),
    from = user.toUserEntity()
)

fun ReactionScore.toReactionScoreEntity(messageId: Long) = ReactionScoreEntity(
    messageId = messageId,
    key = key,
    score = score.toInt()
)

fun ReactionDb.toReaction(): Reaction {
    with(reaction) {
        return Reaction(id, key, score, reason, updateAt, from?.toUser())
    }
}

fun ReactionScoreEntity.toReactionScore(): ReactionScore = ReactionScore(key, score.toLong())
