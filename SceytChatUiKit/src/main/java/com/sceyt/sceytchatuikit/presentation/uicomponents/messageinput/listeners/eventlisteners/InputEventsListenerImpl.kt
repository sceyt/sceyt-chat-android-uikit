package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners

import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView

open class InputEventsListenerImpl(view: MessageInputView) : InputEventsListener.EventListeners {
    private var defaultListeners: InputEventsListener.EventListeners = view
    private var stateListener: InputEventsListener.StateListener? = null

    override fun onStateChanged(state: InputState) {
        defaultListeners.onStateChanged(state)
        stateListener?.onStateChanged(state)
    }

    fun setListener(listener: InputEventsListener) {
        when (listener) {
            is InputEventsListener.EventListeners -> {
                stateListener = listener
                stateListener = listener
            }
            is InputEventsListener.StateListener -> {
                stateListener = listener
            }
        }
    }
}