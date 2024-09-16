package com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.event

sealed interface SearchInputEventListeners {

    fun interface SearchSubmittedListener : SearchInputEventListeners {
        fun onSearchSubmitted(query: String)
    }

    fun interface SearchSubmittedByDebounceListener : SearchInputEventListeners {
        fun onSearchSubmittedByDebounce(query: String)
    }

    /** Use this if you want to implement all callbacks */
    interface EventListeners : SearchSubmittedListener, SearchSubmittedByDebounceListener
}