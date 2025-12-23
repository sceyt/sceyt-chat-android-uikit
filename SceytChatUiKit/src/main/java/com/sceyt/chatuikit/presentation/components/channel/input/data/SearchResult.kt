package com.sceyt.chatuikit.presentation.components.channel.input.data

import com.sceyt.chatuikit.data.models.messages.SceytMessage

data class SearchResult(
    val searchQuery: String,
    val currentIndex: Int = 0,
    val messages: List<SceytMessage> = emptyList(),
    val hasNext: Boolean = false,
    val isLoading: Boolean = false
)
