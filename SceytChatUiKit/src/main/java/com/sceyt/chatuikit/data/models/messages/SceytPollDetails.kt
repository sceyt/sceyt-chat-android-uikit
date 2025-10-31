package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPollDetails(
    val id: String,
    val name: String,
    val messageTid: Long,
    val description: String,
    val options: List<PollOption>,
    val anonymous: Boolean,
    val allowMultipleVotes: Boolean,
    val allowVoteRetract: Boolean,
    val votesPerOption: Map<String, Int>,
    val votes: List<Vote>,
    val ownVotes: List<Vote>,
    val pendingVotes: List<PendingVoteData>?,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long,
    val closed: Boolean,
) : Parcelable {

    val maVotedCountWithPendingVotes: Int
        get() {
            val realCounts = getRealCountsWithPendingVotes()
            return realCounts.maxByOrNull { it.value }?.value ?: 0
        }

    fun getRealCountsWithPendingVotes(): Map<String, Int> {
        val pendingPartition = pendingVotes.orEmpty().partition { it.isAdd }
        if (pendingVotes.isNullOrEmpty()) return votesPerOption
        val (pendingAdd, pendingRemove) = pendingPartition

        // Start with a mutable copy of current vote counts
        val realCounts = votesPerOption.toMutableMap()

        // Add pending vote additions
        pendingAdd.forEach { pending ->
            realCounts[pending.optionId] = (realCounts[pending.optionId] ?: 0) + 1
        }

        // Subtract pending vote removals
        pendingRemove.forEach { pending ->
            realCounts[pending.optionId] =
                ((realCounts[pending.optionId] ?: 0) - 1).coerceAtLeast(0)
        }

        return realCounts
    }
}

@Parcelize
data class PollOption(
    val id: String,
    val name: String,
) : Parcelable

@Parcelize
data class Vote(
    val optionId: String,
    val createdAt: Long,
    val user: SceytUser?,
) : Parcelable

/**
 * UI model for poll option with enriched voting data
 */
data class PollOptionUiModel(
    val id: String,
    val text: String,
    val voteCount: Int,
    val voters: List<SceytUser>,
    val pendingVote: PendingVoteData?,
    val selected: Boolean,
    val totalVotesCount: Int,
) {
    fun getPercentage(): Float {
        return if (totalVotesCount > 0) {
            (voteCount.toFloat() / totalVotesCount.toFloat()) * 100f
        } else 0f
    }
}

/**
 * Converts PollOption to PollOptionUiModel with voting data from SceytPollDetails
 */
fun PollOption.toUiModel(poll: SceytPollDetails): PollOptionUiModel {
    val pendingVote = poll.pendingVotes?.firstOrNull { it.optionId == id }
    val baseCount = poll.votesPerOption[id] ?: 0

    val pendingDelta = if (poll.allowMultipleVotes) {
        when (pendingVote?.isAdd) {
            true -> 1
            false -> -1
            null -> 0
        }
    } else {
        if (poll.pendingVotes.isNullOrEmpty()) {
            0
        } else {
            if (pendingVote != null) {
                if (pendingVote.isAdd) {
                    1
                } else
                    -1
            } else {
                val hasOwnVote = poll.ownVotes.any { it.optionId == id }
                if (hasOwnVote) {
                    -1
                } else {
                    0
                }
            }

        }
    }

    val finalVoteCount = (baseCount + pendingDelta).coerceAtLeast(0)

    val isSelected = when {
        !poll.allowMultipleVotes -> {
            val hasVoted = poll.ownVotes.any { it.optionId == id }
            if (poll.pendingVotes.isNullOrEmpty()) {
                hasVoted
            } else {
                pendingVote?.isAdd ?: false
            }
        }

        else -> {
            (poll.ownVotes.any {
                it.optionId == id
            } && pendingVote == null) || pendingVote?.isAdd == true
        }
    }

    val voters = when {
        poll.anonymous -> emptyList()
        else -> {
            val others = poll.votes.asSequence()
                .filter { it.optionId == id }
                .mapNotNull { it.user }
                .toMutableList()

            val ownVote = poll.ownVotes.firstOrNull { it.optionId == id }?.user

            if (isSelected) {
                listOf(ownVote ?: pendingVote?.user) + others.take(2)
            } else {
                others.take(3)
            }
        }
    }

    return PollOptionUiModel(
        id = id,
        text = name,
        voteCount = finalVoteCount,
        voters = voters.mapNotNull { it },
        selected = isSelected,
        pendingVote = pendingVote,
        totalVotesCount = poll.maVotedCountWithPendingVotes
    )
}

/**
 * Gets all options as UI models with enriched voting data
 */
fun SceytPollDetails.getOptionsUiModels(): List<PollOptionUiModel> {
    return options.map { it.toUiModel(this) }
}