package com.sceyt.chatuikit.data.models

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

data class ChangeVoteResponseData(
    val message: SceytMessage,
    val addedVotes: List<Vote> = emptyList(),
    val removedVotes: List<Vote> = emptyList()
)