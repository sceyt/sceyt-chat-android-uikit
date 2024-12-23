package com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.click

import android.view.View

sealed interface SearchInputClickListeners {

    fun interface ClearClickListener : SearchInputClickListeners {
        fun onClearClick(view: View)
    }

    fun interface KeyboardSearchClickListener : SearchInputClickListeners {
        fun onKeyboardSearchClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : ClearClickListener, KeyboardSearchClickListener
}

internal fun SearchInputClickListeners.setListener(listener: SearchInputClickListeners) {
    (this as? SearchInputClickListenersImpl)?.setListener(listener)
}