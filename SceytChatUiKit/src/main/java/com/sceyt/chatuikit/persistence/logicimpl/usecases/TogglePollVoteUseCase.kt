package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.bumptech.glide.request.RequestOptions.option
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.PendingVoteData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toPendingVoteEntity
import com.sceyt.chatuikit.presentation.extensions.isPending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for toggling poll votes with optimistic UI updates via pending votes.
 * Handles both single-answer and multiple-answer polls with proper server synchronization.
 */
internal class TogglePollVoteUseCase(
    private val addPollVoteUseCase: AddPollVoteUseCase,
    private val removePollVoteUseCase: RemovePollVoteUseCase,
    private val pendingPollVoteDao: PendingPollVoteDao,
    private val messagesCache: MessagesCache,
) {

    /**
     * Toggles vote for a poll option with pending vote support.
     * Updates cache first for immediate UI feedback, then database and server.
     *
     * @param channelId The channel ID
     * @param message The message containing the poll
     * @param option The poll option to vote on
     * @return Updated message with pending vote applied, or null if voting not allowed
     */
    suspend operator fun invoke(
        channelId: Long,
        message: SceytMessage,
        optionId: String,
    ): SceytResponse<SceytMessage> = withContext(Dispatchers.IO) {
        val poll = message.poll
            ?: return@withContext createErrorResponse("Poll not found in message")
        val currentUser = SceytChatUIKit.chatUIFacade.userInteractor.getCurrentUser()
            ?: return@withContext createErrorResponse("Current user not found")

        // Can't vote on closed polls
        if (poll.closed)
            return@withContext createErrorResponse("Poll is closed")

        // Check if there's already a pending votes
        val result = checkPendingVotes(channelId, message, optionId)
        if (result.handled) {
            return@withContext SceytResponse.Success(result.updatedMessage)
        }

        // Determine if user has already voted for this option
        val hasVoted = poll.ownVotes.any { it.optionId == optionId }

        return@withContext if (poll.allowMultipleVotes) {
            // Multiple answer polls - simple toggle
            handleMultipleAnswerToggle(channelId, message, poll, optionId, hasVoted, currentUser)
        } else {
            // Single answer polls - need to handle vote replacement
            handleSingleAnswerToggle(channelId, message, poll, optionId, hasVoted, currentUser)
        }
    }

    /**
     * Handles vote toggle for multiple-answer polls.
     * Simple add or remove based on current vote state.
     */
    private suspend fun handleMultipleAnswerToggle(
        channelId: Long,
        message: SceytMessage,
        poll: SceytPollDetails,
        optionId: String,
        hasVoted: Boolean,
        currentUser: SceytUser,
    ): SceytResponse<SceytMessage> {
        // Create new pending vote
        val newPendingVote = PendingVoteData(
            pollId = poll.id,
            messageTid = message.tid,
            optionId = optionId,
            createdAt = System.currentTimeMillis(),
            isAdd = !hasVoted, // If already voted, we're removing it
            user = currentUser
        )

        // Update message with pending vote
        val updatedPendingVotes = poll.pendingVotes.orEmpty() + newPendingVote
        val updatedMessage = message.copy(
            poll = poll.copy(pendingVotes = updatedPendingVotes)
        )

        // 1. Update cache first for immediate UI responsiveness
        messagesCache.upsertMessages(channelId, updatedMessage)

        // 2. Update database with pending vote
        val pendingVoteEntity = PendingPollVoteEntity(
            messageTid = message.tid,
            pollId = poll.id,
            optionId = optionId,
            userId = currentUser.id,
            isAdd = !hasVoted,
            createdAt = System.currentTimeMillis()
        )
        pendingPollVoteDao.insert(pendingVoteEntity)

        // If message is pending, skip server call
        if (message.isPending())
            return SceytResponse.Success(updatedMessage)

        // 3. Make server call
        return if (hasVoted) {
            removePollVoteUseCase(
                channelId = channelId,
                messageId = message.id,
                pollId = poll.id,
                optionId = optionId,
            )
        } else {
            addPollVoteUseCase(
                channelId = channelId,
                messageId = message.id,
                pollId = poll.id,
                optionId = optionId
            )
        }
    }

    /**
     * Handles vote toggle for single-answer polls.
     * Needs to replace existing votes when selecting a different option.
     */
    private suspend fun handleSingleAnswerToggle(
        channelId: Long,
        message: SceytMessage,
        poll: SceytPollDetails,
        optionId: String,
        hasVoted: Boolean,
        currentUser: SceytUser,
    ): SceytResponse<SceytMessage> {

        // Create new pending vote for clicked option
        val newPendingVote = PendingVoteData(
            pollId = poll.id,
            messageTid = message.tid,
            optionId = optionId,
            createdAt = System.currentTimeMillis(),
            isAdd = !hasVoted, // If already voted, we're removing it
            user = currentUser
        )

        // Update message with pending votes
        val updatedMessage = message.copy(
            poll = poll.copy(pendingVotes = listOf(newPendingVote))
        )

        // 1. Update cache first for immediate UI responsiveness
        messagesCache.upsertMessages(channelId, updatedMessage)

        // Add pending vote for new option
        pendingPollVoteDao.insert(newPendingVote.toPendingVoteEntity())

        // If message is pending, skip server call
        if (message.isPending())
            return SceytResponse.Success(updatedMessage)

        // 3. Make server call
        return if (hasVoted) {
            removePollVoteUseCase(
                channelId = message.channelId,
                messageId = message.id,
                pollId = poll.id,
                optionId = optionId,
            )
        } else {
            addPollVoteUseCase(
                channelId = channelId,
                messageId = message.id,
                pollId = poll.id,
                optionId = optionId
            )
        }
    }

    /**
     * Removes a pending vote when user toggles back before server sync.
     */
    private suspend fun removePendingVote(
        channelId: Long,
        message: SceytMessage,
        optionId: String,
    ): SceytMessage {
        val poll = message.poll ?: return message

        // Remove pending vote from message
        val updatedPendingVotes = poll.pendingVotes.orEmpty().filter {
            it.optionId != optionId
        }

        val updatedMessage = message.copy(
            poll = poll.copy(pendingVotes = updatedPendingVotes)
        )

        // 1. Update cache first
        messagesCache.upsertMessages(channelId, updatedMessage)

        // 2. Remove from database
        pendingPollVoteDao.deleteByOption(message.tid, poll.id, optionId)

        return updatedMessage
    }

    private suspend fun checkPendingVotes(
        channelId: Long,
        message: SceytMessage,
        optionId: String,
    ): CheckPendingResult {
        val defaultResult = CheckPendingResult(
            handled = false,
            updatedMessage = message
        )
        val poll = message.poll
        if (poll == null || poll.pendingVotes.isNullOrEmpty()) {
            return defaultResult
        }

        val pendingVote = poll.pendingVotes.firstOrNull { it.optionId == optionId }

        if (pendingVote != null) {
            val updatedMessage = removePendingVote(channelId, message, optionId)
            return CheckPendingResult(
                handled = true,
                updatedMessage = updatedMessage
            )
        } else {
            if (!poll.allowMultipleVotes) {
                // Remove existing pending votes
                val updatedMessage = message.copy(
                    poll = poll.copy(pendingVotes = null)
                )
                // 1. Update cache first
                messagesCache.upsertMessages(channelId, updatedMessage)

                // 2. Remove from database
                pendingPollVoteDao.deletePendingVotesByPollId(message.tid, poll.id)

                return CheckPendingResult(
                    handled = true,
                    updatedMessage = updatedMessage
                )
            }

        }
        return defaultResult
    }

    data class CheckPendingResult(
        val handled: Boolean,
        val updatedMessage: SceytMessage
    )
}