package com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView

open class SearchInputClickListenersImpl : SearchInputClickListeners.ClickListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: SearchChannelInputView) {
        defaultListeners = view
    }

    private var defaultListeners: SearchInputClickListeners.ClickListeners? = null
    private var clearClickListener: SearchInputClickListeners.ClearClickListener? = null
    private var keyboardSearchClickListener: SearchInputClickListeners.KeyboardSearchClickListener? = null

    override fun onClearClick(view: View) {
        defaultListeners?.onClearClick(view)
        clearClickListener?.onClearClick(view)
    }

    override fun onKeyboardSearchClick() {
        defaultListeners?.onKeyboardSearchClick()
        keyboardSearchClickListener?.onKeyboardSearchClick()
    }

    fun setListener(listener: SearchInputClickListeners) {
        when (listener) {
            is SearchInputClickListeners.ClickListeners -> {
                clearClickListener = listener
                keyboardSearchClickListener = listener
            }

            is SearchInputClickListeners.ClearClickListener -> {
                clearClickListener = listener
            }

            is SearchInputClickListeners.KeyboardSearchClickListener -> {
                keyboardSearchClickListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listeners: SearchInputClickListeners.ClickListeners
    ): SearchInputClickListenersImpl {
        defaultListeners = listeners
        return this
    }
}