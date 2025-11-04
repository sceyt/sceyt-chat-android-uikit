package com.sceyt.chatuikit.presentation.components.poll_results.adapter

import com.sceyt.chatuikit.data.models.messages.Vote

sealed class VoterItem {
    data class HeaderItem(
            val voteCount: Int
    ) : VoterItem()
    
    data class Voter(
            val vote: Vote
    ) : VoterItem()

    data object LoadingMore : VoterItem()
}