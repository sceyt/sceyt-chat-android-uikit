package com.sceyt.chatuikit.presentation.components.channel.messages.helpers

import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.Vote

/**
 * Helper class for managing poll voting logic
 */
object PollVoteHelper {

    /**
     * Toggles a vote for the given option in the poll.
     * Handles both single and multiple choice polls.
     *
     * @param message The message containing the poll
     * @param clickedOption The option that was clicked
     * @param currentUser The user who is voting
     * @return Updated poll with new state, or null if voting failed
     */
    fun toggleVote(message: SceytMessage, clickedOption: PollOptionUiModel, currentUser: SceytUser?): SceytPollDetails? {
        val poll = message.poll ?: return null
        if (poll.closed) return null // Can't vote on closed polls

        currentUser ?: return null

        val isCurrentlyVoted = poll.ownVotes.any { it.optionId == clickedOption.id }

        // Update ownVotes
        val updatedOwnVotes = if (isCurrentlyVoted) {
            // Remove vote
            poll.ownVotes.filter { it.optionId != clickedOption.id }
        } else {
            val newVote = Vote(
                id = java.util.UUID.randomUUID().toString(),
                pollId = poll.id,
                optionId = clickedOption.id,
                createdAt = System.currentTimeMillis(),
                user = currentUser
            )

            if (poll.allowMultipleVotes) {
                // Multiple choice: add to existing votes
                poll.ownVotes + newVote
            } else {
                // Single choice: replace all votes
                listOf(newVote)
            }
        }

        // Update the main votes list (this affects voters display)
        val updatedVotes = poll.votes.toMutableList()

        if (isCurrentlyVoted) {
            // Remove current user's vote from this option
            updatedVotes.removeAll { it.optionId == clickedOption.id && it.user?.id == currentUser.id }
        } else {
            // Add new vote
            val newVote = Vote(
                id = java.util.UUID.randomUUID().toString(),
                pollId = poll.id,
                optionId = clickedOption.id,
                createdAt = System.currentTimeMillis(),
                user = currentUser
            )
            updatedVotes.add(newVote)

            // For single choice, remove user's votes from other options
            if (!poll.allowMultipleVotes) {
                updatedVotes.removeAll { it.user?.id == currentUser.id && it.optionId != clickedOption.id }
            }
        }

        // Update vote counts
        val updatedVotesPerOption = poll.votesPerOption.toMutableMap()

        if (isCurrentlyVoted) {
            updatedVotesPerOption[clickedOption.id] = maxOf(0, (updatedVotesPerOption[clickedOption.id]
                    ?: 0) - 1)
        } else {
            updatedVotesPerOption[clickedOption.id] = (updatedVotesPerOption[clickedOption.id]
                    ?: 0) + 1
        }

        // For single choice, update vote counts for options we removed votes from
        if (!poll.allowMultipleVotes && !isCurrentlyVoted) {
            poll.ownVotes.forEach { oldVote ->
                if (oldVote.optionId != clickedOption.id) {
                    updatedVotesPerOption[oldVote.optionId] = maxOf(0, (updatedVotesPerOption[oldVote.optionId]
                            ?: 0) - 1)
                }
            }
        }

        // Create updated poll
        return poll.copy(
            votes = updatedVotes,
            ownVotes = updatedOwnVotes,
            votesPerOption = updatedVotesPerOption
        )
    }
}

