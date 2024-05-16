package com.sceyt.chatuikit.presentation.uicomponents.messageinput

import com.sceyt.chatuikit.data.models.messages.SceytMessage

data class SearchResult(
        val currentIndex: Int = 0,
        val messages: List<SceytMessage> = emptyList(),
        val hasNext: Boolean = false
)
