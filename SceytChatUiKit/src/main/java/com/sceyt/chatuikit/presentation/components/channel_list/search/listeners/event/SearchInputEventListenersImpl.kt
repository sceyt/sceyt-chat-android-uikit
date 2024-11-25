package com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.event

import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView

open class SearchInputEventListenersImpl : SearchInputEventListeners.EventListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: SearchChannelInputView) {
        defaultListeners = view
    }

    private var defaultListeners: SearchInputEventListeners.EventListeners? = null
    private var searchSubmittedListener: SearchInputEventListeners.SearchSubmittedListener? = null
    private var searchSubmittedByDebounceListener: SearchInputEventListeners.SearchSubmittedByDebounceListener? = null

    override fun onSearchSubmitted(query: String) {
        defaultListeners?.onSearchSubmitted(query)
        searchSubmittedListener?.onSearchSubmitted(query)
    }

    override fun onSearchSubmittedByDebounce(query: String) {
        defaultListeners?.onSearchSubmittedByDebounce(query)
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

    internal fun withDefaultListeners(
            listeners: SearchInputEventListeners.EventListeners
    ): SearchInputEventListenersImpl {
        defaultListeners = listeners
        return this
    }
}