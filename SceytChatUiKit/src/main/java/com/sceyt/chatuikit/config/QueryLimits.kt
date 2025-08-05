package com.sceyt.chatuikit.config

import androidx.annotation.IntRange

data class QueryLimits(
        @IntRange(1, 50) val channelListQueryLimit: Int = 20,
        @IntRange(1, 50) val channelMemberListQueryLimit: Int = 30,
        @IntRange(1, 50) val userListQueryLimit: Int = 30,
        @IntRange(1, 50) val messageListQueryLimit: Int = 50,
        @IntRange(1, 50) val attachmentListQueryLimit: Int = 20,
        @IntRange(1, 50) val reactionListQueryLimit: Int = 30,
        @IntRange(1, 50) val unreadMentionsListQueryLimit: Int = 30,
)