package com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.listeners

sealed interface SearchInputEventListeners {

    fun interface SearchSubmittedListener : SearchInputEventListeners {
        fun onSearchSubmitted(query: String)
    }

    fun interface SearchSubmittedByDebounceListener : SearchInputEventListeners {
        fun onSearchSubmittedByDebounce(query: String)
    }

    /** User this if you want to implement all callbacks */
    interface EventListeners : SearchSubmittedListener, SearchSubmittedByDebounceListener
}