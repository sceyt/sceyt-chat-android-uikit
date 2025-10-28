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
)

internal fun Vote.toPollVoteEntity() = PollVoteEntity(
    id = id,
    pollId = pollId,
    optionId = optionId,
    userId = user?.id ?: "",
    createdAt = createdAt,
)

internal fun SceytPollDetails.toPollDb() = PollDb(
    pollEntity = toPollEntity(messageTid),
    options = options.map { it.toPollOptionEntity(id) },
    votes = votes.mapNotNull { vote ->
        vote.user?.let { user ->
            PollVoteDb(
                vote = vote.toPollVoteEntity(),
                user = user.toUserDb()
            )
        }
    },
    pendingVotes = null
)

internal fun PollOptionEntity.toPollOption() = PollOption(
    id = id,
    name = name,
)

internal fun PollVoteDb.toVote() = Vote(
    id = vote.id,
    pollId = vote.pollId,
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

internal fun PollDb.toSceytPollDetails() = SceytPollDetails(
    id = pollEntity.id,
    name = pollEntity.name,
    messageTid = pollEntity.messageTid,
    description = pollEntity.description,
    options = options.map { it.toPollOption() },
    anonymous = pollEntity.anonymous,
    allowMultipleVotes = pollEntity.allowMultipleVotes,
    allowVoteRetract = pollEntity.allowVoteRetract,
    votesPerOption = pollEntity.votesPerOption,
    votes = votes?.map { it.toVote() }.orEmpty(),
    ownVotes = votes?.filter { it.user?.id == SceytChatUIKit.currentUserId }?.map { it.toVote() }.orEmpty(),
    pendingVotes = pendingVotes?.map { it.toPendingVoteData() },
    createdAt = pollEntity.createdAt,
    updatedAt = pollEntity.updatedAt,
    closedAt = pollEntity.closedAt,
    closed = pollEntity.closed,
)

internal fun PollDetails.toSceytPollDetails(
        messageTid: Long,
) = SceytPollDetails(
    id = id,
    name = name,
    messageTid = messageTid,
    description = description.orEmpty(),
    options = options.map { option -> option.toPollOption() },
    anonymous = isAnonymous,
    allowMultipleVotes = isAllowMultipleVotes,
    allowVoteRetract = isAllowVoteRetract,
    votesPerOption = votesPerOption,
    votes = votes.map { vote -> vote.toVote() },
    ownVotes = ownVotes.map { it.toVote() },
    pendingVotes = null,
    createdAt = createdAt.time,
    updatedAt = updatedAt.time,
    closedAt = closedAt.time,
    closed = isClosed,
)

internal fun PollVote.toVote() = Vote(
    id = id,
    pollId = pollId,
    optionId = optionId,
    createdAt = createdAt.time,
    user = user?.toSceytUser(),
)

internal fun com.sceyt.chat.models.poll.PollOption.toPollOption() = PollOption(
    id = id,
    name = name,
)