package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
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

    @IgnoredOnParcel
    val totalVotes: Int by lazy {
        votesPerOption.values.sum()
    }

    val totalVotesWithPendingVotes: Int
        get() {
            return if (allowMultipleVotes) {
                val pendingAdds = pendingVotes?.count { it.isAdd } ?: 0
                val pendingRemoves = pendingVotes?.count { !it.isAdd } ?: 0
                (totalVotes + pendingAdds - pendingRemoves).coerceAtLeast(0)
            } else {
                val hasPendingAdd = pendingVotes?.any { it.isAdd } == true
                val hasPendingRemove = pendingVotes?.any { !it.isAdd } == true
                when {
                    hasPendingAdd -> if (ownVotes.isEmpty()) totalVotes + 1 else totalVotes
                    hasPendingRemove -> (totalVotes - 1).coerceAtLeast(0)
                    else -> totalVotes
                }
            }
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

        else -> poll.ownVotes.any {
            it.optionId == id
        } || pendingVote?.isAdd == true
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
        totalVotesCount = poll.totalVotesWithPendingVotes
    )
}

/**
 * Gets all options as UI models with enriched voting data
 */
fun SceytPollDetails.getOptionsUiModels(): List<PollOptionUiModel> {
    return options.map { it.toUiModel(this) }
}