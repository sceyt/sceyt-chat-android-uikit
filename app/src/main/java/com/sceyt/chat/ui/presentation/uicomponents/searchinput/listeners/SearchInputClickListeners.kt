package com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners

import android.view.View

sealed interface SearchInputClickListeners {

    fun interface ClearClickListener : SearchInputClickListeners {
        fun onClearClick(view: View)
    }

    fun interface KeyboardSearchClickListener : SearchInputClickListeners {
        fun onKeyboardSearchClick()
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners : ClearClickListener, KeyboardSearchClickListener
}