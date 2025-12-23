package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity

internal class SendPollPendingVotesUseCase(
    private val addPollVoteUseCase: AddPollVoteUseCase,
    private val removePollVoteUseCase: RemovePollVoteUseCase,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageId: Long,
        pollId: String,
        pendingVotes: List<PendingPollVoteEntity>,
    ) {

        val (addVotes, deleteVoted) = pendingVotes.partition { it.isAdd }

        if (addVotes.isNotEmpty()) {
            addPollVoteUseCase(
                channelId = channelId,
                messageId = messageId,
                pollId = pollId,
                optionIds = addVotes.map { it.optionId }
            )
        }

        if (deleteVoted.isNotEmpty()) {
            removePollVoteUseCase(
                channelId = channelId,
                messageId = messageId,
                pollId = pollId,
                optionIds = deleteVoted.map { it.optionId }
            )
        }
    }
}