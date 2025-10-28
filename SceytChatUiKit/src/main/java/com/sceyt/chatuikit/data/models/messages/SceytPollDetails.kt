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
        votesPerOption.values.sum() + (pendingVotes?.sumOf {
            if (it.isAdd) 1 else -1
        } ?: 0)
    }
}

@Parcelize
data class PollOption(
        val id: String,
        val name: String,
) : Parcelable

@Parcelize
data class Vote(
        val id: String,
        val pollId: String,
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
) {
    fun getPercentage(totalVotes: Int): Float {
        return if (totalVotes > 0) {
            (voteCount.toFloat() / totalVotes.toFloat()) * 100f
        } else 0f
    }
}

/**
 * Converts PollOption to PollOptionUiModel with voting data from SceytPollDetails
 */
fun PollOption.toUiModel(poll: SceytPollDetails): PollOptionUiModel {
    val pendingVote = poll.pendingVotes?.firstOrNull { it.optionId == id }
    val pendingDelta = when (pendingVote?.isAdd) {
        true -> 1
        false -> -1
        null -> 0
    }

    val baseCount = poll.votesPerOption[id] ?: 0
    val finalVoteCount = (baseCount + pendingDelta).coerceAtLeast(0)

    val isSelected = poll.ownVotes.any { it.optionId == id } || pendingVote?.isAdd == true

    val voters = when {
        poll.anonymous -> emptyList()
        else -> {
            val current = poll.votes.asSequence()
                .filter { it.optionId == id }
                .mapNotNull { it.user }
                .toList()

            when {
                pendingVote == null -> current
                pendingVote.isAdd -> current + pendingVote.user
                else -> current.filter { it.id != pendingVote.user.id }
            }
        }
    }

    return PollOptionUiModel(
        id = id,
        text = name,
        voteCount = finalVoteCount,
        voters = voters,
        selected = isSelected,
        pendingVote = pendingVote
    )
}

/**
 * Gets all options as UI models with enriched voting data
 */
fun SceytPollDetails.getOptionsUiModels(): List<PollOptionUiModel> {
    return options.map { it.toUiModel(this) }
}