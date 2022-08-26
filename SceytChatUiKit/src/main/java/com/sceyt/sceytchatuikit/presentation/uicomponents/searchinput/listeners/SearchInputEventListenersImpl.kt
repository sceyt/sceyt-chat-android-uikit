package com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.listeners

import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView

open class SearchInputEventListenersImpl(view: SearchInputView) : SearchInputEventListeners.EventListeners {
    private var defaultListeners: SearchInputEventListeners.EventListeners = view
    private var searchSubmittedListener: SearchInputEventListeners.SearchSubmittedListener? = null
    private var searchSubmittedByDebounceListener: SearchInputEventListeners.SearchSubmittedByDebounceListener? = null

    override fun onSearchSubmitted(query: String) {
        defaultListeners.onSearchSubmitted(query)
        searchSubmittedListener?.onSearchSubmitted(query)
    }

    override fun onSearchSubmittedByDebounce(query: String) {
        defaultListeners.onSearchSubmittedByDebounce(query)
        searchSubmittedByDebounceListener?.onSearchSubmittedByDebounce(query)
    }

    fun setListener(listener: SearchInputEventListeners) {
        when (listener) {
            is SearchInputEventListeners.EventListeners -> {
                searchSubmittedListener = listener
                searchSubmittedByDebounceListener = listener
            }
            is SearchInputEventListeners.SearchSubmittedListener -> {
                searchSubmittedListener = listener
            }
            is SearchInputEventListeners.SearchSubmittedByDebounceListener -> {
                searchSubmittedByDebounceListener = listener
            }
        }
    }
}