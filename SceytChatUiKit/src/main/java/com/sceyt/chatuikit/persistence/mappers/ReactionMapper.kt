package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.PendingReactionData
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.persistence.entity.channel.ChatUserReactionDb
import com.sceyt.chatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.chatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.chatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.entity.messages.ReactionTotalEntity
import com.sceyt.chatuikit.persistence.entity.pendings.PendingReactionEntity

fun SceytReaction.toReactionEntity() = ReactionEntity(
    id = id,
    messageId = messageId,
    key = key,
    score = score,
    reason = reason,
    createdAt = createdAt,
    fromId = user?.id
)

fun SceytReaction.toReaction() = Reaction(
    id,
    messageId,
    key,
    score,
    reason,
    createdAt,
    user
)

fun SceytReaction.toReactionDb() = ReactionDb(
    reaction = toReactionEntity(),
    from = user?.toUserEntity()
)

fun ReactionTotal.toReactionTotalEntity(messageId: Long) = ReactionTotalEntity(
    messageId = messageId,
    key = key,
    count = count,
    score = score.toInt()
)

fun ReactionDb.toSceytReaction(): SceytReaction {
    with(reaction) {
        return SceytReaction(id, messageId, key, score, reason, createdAt, from?.toUser(), pending = false)
    }
}

fun PendingReactionEntity.toSceytReaction() = SceytReaction(
    id = id,
    messageId = messageId,
    key = key,
    score = score,
    reason = "",
    createdAt = createdAt,
    user = ClientWrapper.currentUser ?: User(SceytChatUIKit.chatUIFacade.myId),
    pending = true
)

fun ReactionDb.toReaction(): Reaction {
    with(reaction) {
        return Reaction(id, messageId, key, score, reason, createdAt, from?.toUser())
    }
}

fun ChatUserReactionDb.toSceytReaction(): SceytReaction {
    with(reaction) {
        return SceytReaction(id, messageId, key, score, reason, createdAt, from?.toUser(), pending = false)
    }
}

fun SceytReaction.toUserReactionsEntity(channelId: Long) = ChatUserReactionEntity(
    id = id,
    messageId = messageId,
    channelId = channelId,
    key = key,
    score = score,
    reason = reason,
    createdAt = createdAt,
    fromId = user?.id
)

fun Reaction.toSceytReaction() = SceytReaction(id, messageId, key, score, reason, createdAt.time, user, false)

fun ReactionTotalEntity.toReactionTotal(): ReactionTotal = ReactionTotal(key, count, score.toLong())

fun PendingReactionEntity.toReactionData() = PendingReactionData(messageId, key, score, count, createdAt, isAdd, incomingMsg)

fun PendingReactionData.toSceytReaction() = SceytReaction(0, messageId, key, score, "", createdAt, ClientWrapper.currentUser
        ?: User(SceytChatUIKit.chatUIFacade.myId), true)
