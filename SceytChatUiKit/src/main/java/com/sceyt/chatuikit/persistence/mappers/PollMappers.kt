package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.poll.PollDetails
import com.sceyt.chat.models.poll.PollVote
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.PendingVoteData
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.persistence.database.entity.messages.PendingPollVoteDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PollDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PollEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollOptionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollVoteDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PollVoteEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity

internal fun SceytPollDetails.toPollEntity(messageTid: Long) = PollEntity(
    id = id,
    messageTid = messageTid,
    name = name,
    description = description,
    anonymous = anonymous,
    allowMultipleVotes = allowMultipleVotes,
    votesPerOption = votesPerOption,
    allowVoteRetract = allowVoteRetract,
    createdAt = createdAt,
    updatedAt = updatedAt,
    closedAt = closedAt,
    closed = closed,
)

internal fun PollOption.toPollOptionEntity(pollId: String) = PollOptionEntity(
    id = id,
    pollId = pollId,
    name = name,
    order = order
)

internal fun Vote.toPollVoteEntity(pollId: String) = PollVoteEntity(
    pollId = pollId,
    optionId = optionId,
    userId = user?.id ?: "",
    createdAt = createdAt,
)

internal fun SceytPollDetails.toPollDb() = PollDb(
    pollEntity = toPollEntity(messageTid),
    options = options.map { it.toPollOptionEntity(id) },
    votes = (votes + ownVotes).mapNotNull { vote ->
        vote.user?.let { user ->
            PollVoteDb(
                vote = vote.toPollVoteEntity(id),
                user = user.toUserDb()
            )
        }
    },
    pendingVotes = null
)

internal fun PollOptionEntity.toPollOption() = PollOption(
    id = id,
    name = name,
    order = order
)

internal fun PollVoteDb.toVote() = Vote(
    optionId = vote.optionId,
    createdAt = vote.createdAt,
    user = user?.toSceytUser(),
)

internal fun PendingPollVoteDb.toPendingVoteData() = with(pendingVote) {
    PendingVoteData(
        messageTid = messageTid,
        pollId = pollId,
        optionId = optionId,
        user = user?.toSceytUser() ?: SceytUser(userId),
        isAdd = isAdd,
        createdAt = createdAt
    )
}

internal fun PollDb.toSceytPollDetails(): SceytPollDetails {
    val myId = SceytChatUIKit.currentUserId
    val ownVotes = mutableListOf<Vote>()
    val otherVotes = mutableListOf<Vote>()
    votes?.forEach {
        if (it.vote.userId == myId) {
            ownVotes.add(it.toVote())
        } else {
            otherVotes.add(it.toVote())
        }
    }

    return SceytPollDetails(
        id = pollEntity.id,
        name = pollEntity.name,
        messageTid = pollEntity.messageTid,
        description = pollEntity.description,
        options = options.map { it.toPollOption() },
        anonymous = pollEntity.anonymous,
        allowMultipleVotes = pollEntity.allowMultipleVotes,
        allowVoteRetract = pollEntity.allowVoteRetract,
        votesPerOption = pollEntity.votesPerOption,
        votes = otherVotes,
        ownVotes = ownVotes,
        pendingVotes = pendingVotes?.map { it.toPendingVoteData() },
        createdAt = pollEntity.createdAt,
        updatedAt = pollEntity.updatedAt,
        closedAt = pollEntity.closedAt,
        closed = pollEntity.closed,
    )
}

internal fun PollDetails.toSceytPollDetails(
    messageTid: Long,
) = SceytPollDetails(
    id = id,
    name = name,
    messageTid = messageTid,
    description = description.orEmpty(),
    options = options?.mapIndexed { index, option -> option.toPollOption(index) }.orEmpty(),
    anonymous = isAnonymous,
    allowMultipleVotes = isAllowMultipleVotes,
    allowVoteRetract = isAllowVoteRetract,
    votesPerOption = votesPerOption,
    votes = votes?.map { vote -> vote.toVote() }.orEmpty(),
    ownVotes = ownVotes?.map { it.toVote() }.orEmpty(),
    pendingVotes = null,
    createdAt = createdAt.time,
    updatedAt = updatedAt.time,
    closedAt = closedAt.time,
    closed = isClosed,
)

internal fun PendingVoteData.toPendingVoteEntity() = PendingPollVoteEntity(
    messageTid = messageTid,
    pollId = pollId,
    optionId = optionId,
    userId = user.id,
    isAdd = isAdd,
    createdAt = createdAt
)

internal fun PollVote.toVote() = Vote(
    optionId = optionId,
    createdAt = createdAt.time,
    user = user?.toSceytUser(),
)

internal fun com.sceyt.chat.models.poll.PollOption.toPollOption(order: Int) = PollOption(
    id = id,
    name = name,
    order = order
)