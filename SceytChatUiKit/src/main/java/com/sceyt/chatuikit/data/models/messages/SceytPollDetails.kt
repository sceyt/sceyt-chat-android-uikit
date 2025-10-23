package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPollDetails(
        val id: String,
        val name: String,
        val description: String,
        val options: List<PollOption>,
        val anonymous: Boolean,
        val allowMultipleVotes: Boolean,
        val allowVoteRetract: Boolean,
        val votesPerOption: Map<String, Int>,
        val votes: List<Vote>,
        val ownVotes: List<Vote>,
        val createdAt: Long,
        val updatedAt: Long,
        val closedAt: Long,
        val closed: Boolean,
) : Parcelable {

    @IgnoredOnParcel
    val totalVotes: Int by lazy {
        votesPerOption.values.sum()
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
    val voteCount = poll.votesPerOption[id] ?: 0
    val optionVotes = poll.votes.filter { it.optionId == id }
    val voters = if (!poll.anonymous) optionVotes.mapNotNull { it.user } else emptyList()
    val isSelected = poll.ownVotes.any { it.optionId == id }
    
    return PollOptionUiModel(
        id = id,
        text = name,
        voteCount = voteCount,
        voters = voters,
        selected = isSelected
    )
}

/**
 * Gets all options as UI models with enriched voting data
 */
fun SceytPollDetails.getOptionsUiModels(): List<PollOptionUiModel> {
    return options.map { it.toUiModel(this) }
}