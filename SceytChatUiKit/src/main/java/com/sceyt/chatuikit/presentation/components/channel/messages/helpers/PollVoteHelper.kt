package com.sceyt.chatuikit.presentation.components.channel.messages.helpers

import android.system.Os.poll
import com.google.gson.Gson
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.data.models.messages.SceytUser

/**
 * Helper class for managing poll voting logic
 */
object PollVoteHelper {
    private val gson = Gson()

    /**
     * Toggles a vote for the given option in the poll.
     * Handles both single and multiple choice polls.
     * 
     * @param message The message containing the poll
     * @param clickedOption The option that was clicked
     * @param currentUser The user who is voting
     * @return Updated poll with new state, or null if voting failed
     */
    fun toggleVote(message: SceytMessage, clickedOption: PollOption, currentUser: SceytUser?): SceytPoll? {
        val poll = parsePoll(message.metadata) ?: return null
        if (poll.closed) return null // Can't vote on closed polls
        
        val updatedOptions = if (poll.allowMultipleAnswers) {
            // Multiple choice: toggle the clicked option
            handleMultipleChoice(poll.options, clickedOption, currentUser)
        } else {
            // Single choice: unselect others and toggle the clicked option
            handleSingleChoice(poll.options, clickedOption, currentUser)
        }
        
        // Calculate new total votes
        val newTotalVotes = updatedOptions.sumOf { it.voteCount }
        
        // Create updated poll
        return poll.copy(
            options = updatedOptions,
            totalVotes = newTotalVotes
        )
    }

    fun toggleVote(poll: SceytPoll, clickedOption: PollOption, currentUser: SceytUser?): SceytPoll? {
        if (poll.closed) return null // Can't vote on closed polls

        val updatedOptions = if (poll.allowMultipleAnswers) {
            // Multiple choice: toggle the clicked option
            handleMultipleChoice(poll.options, clickedOption, currentUser)
        } else {
            // Single choice: unselect others and toggle the clicked option
            handleSingleChoice(poll.options, clickedOption, currentUser)
        }

        // Calculate new total votes
        val newTotalVotes = updatedOptions.sumOf { it.voteCount }

        // Create updated poll
        return poll.copy(
            options = updatedOptions,
            totalVotes = newTotalVotes
        )
    }
    
    /**
     * Handles voting in single choice polls
     */
    private fun handleSingleChoice(
        options: List<PollOption>,
        clickedOption: PollOption,
        currentUser: SceytUser?
    ): List<PollOption> {
        return options.map { option ->
            when {
                option.id == clickedOption.id -> {
                    // Toggle the clicked option
                    if (option.selected) {
                        // Unvote
                        option.copy(
                            selected = false,
                            voteCount = maxOf(0, option.voteCount - 1),
                            voters = option.voters.filter { it.id != currentUser?.id }
                        )
                    } else {
                        // Vote
                        val newVoters = if (currentUser != null && !option.voters.any { it.id == currentUser.id }) {
                            option.voters + currentUser
                        } else {
                            option.voters
                        }
                        option.copy(
                            selected = true,
                            voteCount = option.voteCount + 1,
                            voters = newVoters
                        )
                    }
                }
                option.selected -> {
                    // Unselect other options
                    option.copy(
                        selected = false,
                        voteCount = maxOf(0, option.voteCount - 1),
                        voters = option.voters.filter { it.id != currentUser?.id }
                    )
                }
                else -> option
            }
        }
    }
    
    /**
     * Handles voting in multiple choice polls
     */
    private fun handleMultipleChoice(
        options: List<PollOption>,
        clickedOption: PollOption,
        currentUser: SceytUser?
    ): List<PollOption> {
        return options.map { option ->
            if (option.id == clickedOption.id) {
                // Toggle only the clicked option
                if (option.selected) {
                    // Unvote
                    option.copy(
                        selected = false,
                        voteCount = maxOf(0, option.voteCount - 1),
                        voters = option.voters.filter { it.id != currentUser?.id }
                    )
                } else {
                    // Vote
                    val newVoters = if (currentUser != null && !option.voters.any { it.id == currentUser.id }) {
                        option.voters + currentUser
                    } else {
                        option.voters
                    }
                    option.copy(
                        selected = true,
                        voteCount = option.voteCount + 1,
                        voters = newVoters
                    )
                }
            } else {
                option
            }
        }
    }
    
    /**
     * Parses poll data from message metadata
     */
    fun parsePoll(metadata: String?): SceytPoll? {
        return try {
            metadata?.let { gson.fromJson(it, SceytPoll::class.java) }
        } catch (e: Exception) {
            null
        }
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

