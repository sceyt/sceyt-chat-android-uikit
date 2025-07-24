package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

data class UnreadMentionState(
        val messageIds: Set<Long> = emptySet(),
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
)