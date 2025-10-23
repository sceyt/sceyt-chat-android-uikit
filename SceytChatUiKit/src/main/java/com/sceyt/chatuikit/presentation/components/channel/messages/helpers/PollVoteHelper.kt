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
        
        // Update vote counts
        val updatedVotesPerOption = poll.votesPerOption.toMutableMap()
        
        // Handle vote removal
        if (isCurrentlyVoted) {
            updatedVotesPerOption[clickedOption.id] = maxOf(0, (updatedVotesPerOption[clickedOption.id] ?: 0) - 1)
        } else {
            updatedVotesPerOption[clickedOption.id] = (updatedVotesPerOption[clickedOption.id] ?: 0) + 1
        }
        
        // For single choice, remove votes from other options
        if (!poll.allowMultipleVotes && !isCurrentlyVoted) {
            poll.ownVotes.forEach { oldVote ->
                if (oldVote.optionId != clickedOption.id) {
                    updatedVotesPerOption[oldVote.optionId] = maxOf(0, (updatedVotesPerOption[oldVote.optionId] ?: 0) - 1)
                }
            }
        }
        
        // Create updated poll
        return poll.copy(
            ownVotes = updatedOwnVotes,
            votesPerOption = updatedVotesPerOption
        )
    }
    
    /**
     * Gets the current user from the message or creates a mock user
     */
    fun getCurrentUser(): SceytUser {
        // In a real app, this would get the actual current user
        // For now, returning a mock user for demonstration
        return SceytUser(
            id = "currentUser"
        ).copy(
            firstName = "You",
            lastName = "",
        )
    }
}

