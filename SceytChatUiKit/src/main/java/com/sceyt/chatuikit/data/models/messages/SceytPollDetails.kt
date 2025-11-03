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
}


private fun SceytPollDetails.getRealCountsWithPendingVotes(): Map<String, Int> {
    if (pendingVotes.isNullOrEmpty()) return votesPerOption

    val (pendingAdd, pendingRemove) = pendingVotes.partition { it.isAdd }

    if (pendingAdd.isEmpty() && pendingRemove.isEmpty())
        return votesPerOption

    // Start with a mutable copy of current vote counts
    val realCounts = votesPerOption.toMutableMap()

    // Add pending vote additions
    pendingAdd.forEach { pending ->
        realCounts[pending.optionId] = (realCounts[pending.optionId] ?: 0) + 1
    }
    // Subtract pending vote removals
    pendingRemove.forEach { pending ->
        realCounts[pending.optionId] = realCounts
            .getOrDefault(pending.optionId, 0)
            .minus(1)
            .coerceAtLeast(0)
    }

    // Handle single-vote polls: ensure only one option is selected
    if (!allowMultipleVotes && pendingAdd.isNotEmpty()) {
        val pendingAddOptionIds = pendingAdd.map { it.optionId }.toSet()
        ownVotes.forEach {
            if (it.optionId !in pendingAddOptionIds) {
                realCounts[it.optionId] = (realCounts[it.optionId] ?: 1) - 1
                    .coerceAtLeast(0)
            }
        }
    }

    return realCounts
}