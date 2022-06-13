package com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.SearchInputView

class SearchInputClickListenersImpl(view: SearchInputView) : SearchInputClickListeners.ClickListeners {
    private var defaultListeners: SearchInputClickListeners.ClickListeners = view
    private var clearClickListener: SearchInputClickListeners.ClearClickListener? = null
    private var keyboardSearchClickListener: SearchInputClickListeners.KeyboardSearchClickListener? = null

    override fun onClearClick(view: View) {
        defaultListeners.onClearClick(view)
        clearClickListener?.onClearClick(view)
    }

    override fun onKeyboardSearchClick() {
        defaultListeners.onKeyboardSearchClick()
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
}