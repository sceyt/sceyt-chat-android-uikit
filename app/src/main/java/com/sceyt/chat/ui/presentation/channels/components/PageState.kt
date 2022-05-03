package com.sceyt.chat.ui.presentation.channels.components

data class PageState(
        val query: String,
        val isLoading: Boolean,
        val isLoadingMore: Boolean,
        val isEmpty: Boolean,
        val isSearch: Boolean = query.isNotBlank()
)