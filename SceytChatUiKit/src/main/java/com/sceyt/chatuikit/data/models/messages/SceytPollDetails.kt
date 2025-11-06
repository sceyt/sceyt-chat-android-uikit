package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chatuikit.persistence.extensions.getRealCountsWithPendingVotes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPollDetails(
    val id: String,
    val messageTid: Long,
    val name: String,
    val description: String,
    val anonymous: Boolean,
    val allowMultipleVotes: Boolean,
    val allowVoteRetract: Boolean,
    val votesPerOption: Map<String, Int>,
    val options: List<PollOption>,
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