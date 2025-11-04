package com.sceyt.chatuikit.presentation.components.poll_results.adapter

import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.Vote

sealed class PollResultItem {
    data class HeaderItem(
            val poll: SceytPollDetails
    ) : PollResultItem()
    
    data class PollOptionItem(
            val pollOption: PollOption,
            val voteCount: Int,
            val voters: List<Vote>,
            val hasMore: Boolean
    ) : PollResultItem()
}