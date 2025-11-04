package com.sceyt.chatuikit.persistence.extensions

import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import java.util.UUID

internal fun SceytPollDetails.getRealCountsWithPendingVotes(): Map<String, Int> {
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

fun SceytPollDetails?.newPoll(): SceytPollDetails? {
    this ?: return null
    return this.copy(
        id = UUID.randomUUID().toString(),
        votesPerOption = emptyMap(),
        votes = emptyList(),
        ownVotes = emptyList(),
        options = options.map {
            it.copy(id = UUID.randomUUID().toString())
        },
        pendingVotes = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = 0L,
        closedAt = 0L,
        closed = false
    )
}