package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a poll in a message
 * @property id Unique identifier for the poll
 * @property question The poll question/title
 * @property options List of poll options
 * @property allowMultipleAnswers Whether users can select multiple options
 * @property anonymous Whether votes are anonymous
 * @property allowAddOption Whether users can add their own options
 * @property endAt Poll end timestamp (null if no expiration)
 * @property createdAt Poll creation timestamp
 * @property totalVotes Total number of votes cast
 */
@Parcelize
data class SceytPoll(
        val id: String,
        val question: String,
        val options: List<PollOption>,
        val allowMultipleAnswers: Boolean = false,
        val anonymous: Boolean = false,
        val allowAddOption: Boolean = false,
        val endAt: Long? = null,
        val createdAt: Long,
        val totalVotes: Int = 0,
        val closed: Boolean = false
) : Parcelable

/**
 * Represents a single option in a poll
 * @property id Unique identifier for the option
 * @property text The option text
 * @property voteCount Number of votes for this option
 * @property voters List of users who voted for this option (empty if anonymous)
 * @property selected Whether the current user selected this option
 */
@Parcelize
data class PollOption(
        val id: String,
        val text: String,
        val voteCount: Int = 0,
        val voters: List<SceytUser> = emptyList(),
        val selected: Boolean = false
) : Parcelable {
    
    /**
     * Calculate percentage of total votes
     */
    fun getPercentage(totalVotes: Int): Float {
        return if (totalVotes > 0) {
            (voteCount.toFloat() / totalVotes.toFloat()) * 100f
        } else 0f
    }
}

