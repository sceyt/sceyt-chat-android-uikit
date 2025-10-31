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
    private val realCountsCache: Map<String, Int> by lazy {
        getRealCountsWithPendingVotes()
    }

    @IgnoredOnParcel
    val maxVotedCountWithPendingVotes: Int
        get() = realCountsCache.maxOfOrNull { it.value } ?: 0

    fun getRealCountsWithPendingVotes(): Map<String, Int> {
        if (pendingVotes.isNullOrEmpty()) return votesPerOption

        val (pendingAdd, pendingRemove) = pendingVotes.partition { it.isAdd }

        // Start with a mutable copy of current vote counts
        val realCounts = votesPerOption.toMutableMap()

        // Add pending vote additions
        pendingAdd.forEach { pending ->
            realCounts[pending.optionId] = (realCounts[pending.optionId] ?: 0) + 1
        }
        // Subtract pending vote removals
        pendingRemove.forEach { pending ->
            realCounts[pending.optionId] =
                (realCounts.getOrDefault(pending.optionId, 0) - 1).coerceAtLeast(0)
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
 * Gets all options as UI models with enriched voting data
 */
fun SceytPollDetails.getOptionsUiModels(): List<PollOptionUiModel> {
    return options.map { it.toUiModel(this) }
}

/**
 * Converts PollOption to PollOptionUiModel with voting data from SceytPollDetails
 */
fun PollOption.toUiModel(poll: SceytPollDetails): PollOptionUiModel {
    val pendingVote = poll.pendingVotes?.firstOrNull { it.optionId == id }
    val baseCount = poll.votesPerOption[id] ?: 0
    val ownVote by lazy { poll.ownVotes.firstOrNull { it.optionId == id } }
    val hasOwnVote by lazy { ownVote != null }

    val pendingDelta = calculatePendingDelta(
        poll = poll,
        pendingVote = pendingVote,
        hasOwnVote = hasOwnVote
    )
    
    val finalVoteCount = (baseCount + pendingDelta).coerceAtLeast(0)

    val isSelected = isOptionSelected(
        poll = poll,
        pendingVote = pendingVote,
        hasOwnVote = hasOwnVote
    )

    // Build list of voter avatars to display (empty if anonymous poll)
    val voters = if (poll.anonymous) {
        emptyList()
    } else {
        val otherVoters = poll.votes
            .filter { it.optionId == id }
            .mapNotNull { it.user }

        if (isSelected) {
            // Show own user first, then up to 2 others
            listOfNotNull(ownVote?.user ?: pendingVote?.user) + otherVoters.take(2)
        } else {
            // Show up to 3 other voters
            otherVoters.take(3)
        }
    }

    return PollOptionUiModel(
        id = id,
        text = name,
        voteCount = finalVoteCount,
        voters = voters,
        selected = isSelected,
        pendingVote = pendingVote,
        totalVotesCount = poll.maxVotedCountWithPendingVotes
    )
}


/**
 * Calculates the pending vote delta for a specific option.
 * For multiple-answer polls: simple +1/-1 logic.
 * For single-answer polls: accounts for deselection of previously selected option when selecting a new one.
 */
@Suppress("UnusedReceiverParameter")
private fun PollOption.calculatePendingDelta(
    poll: SceytPollDetails,
    pendingVote: PendingVoteData?,
    hasOwnVote: Boolean
): Int = when {
    // Multiple votes allowed: simple add/remove logic
    poll.allowMultipleVotes -> when (pendingVote?.isAdd) {
        true -> 1
        false -> -1
        null -> 0
    }

    // Single answer: no pending votes
    poll.pendingVotes.isNullOrEmpty() -> 0

    // Single answer with pending vote for this option
    pendingVote != null -> if (pendingVote.isAdd) 1 else -1

    // Single answer: pending vote for different option, so this option loses vote if previously selected
    else -> if (hasOwnVote) -1 else 0
}

/**
 * Determines if this option is currently selected (considering pending votes).
 * For single-answer polls: only one option can be selected at a time.
 * For multiple-answer polls: multiple options can be selected simultaneously.
 */
@Suppress("UnusedReceiverParameter")
private fun PollOption.isOptionSelected(
    poll: SceytPollDetails,
    pendingVote: PendingVoteData?,
    hasOwnVote: Boolean
): Boolean = when {
    // Single answer poll
    !poll.allowMultipleVotes -> {
        if (poll.pendingVotes.isNullOrEmpty()) {
            // No pending votes: check if user previously voted for this option
            hasOwnVote
        } else {
            // Pending votes exist: only the pending add vote determines selection
            pendingVote?.isAdd ?: false
        }
    }

    // Multiple votes poll: selected if voted and not pending removal, OR pending add
    else -> (hasOwnVote && pendingVote == null) || pendingVote?.isAdd == true
}