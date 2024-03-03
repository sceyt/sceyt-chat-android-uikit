package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

data class SearchResult(
        val currentIndex: Int = 0,
        val messages: List<SceytMessage> = emptyList(),
        val hasNext: Boolean = false
)
