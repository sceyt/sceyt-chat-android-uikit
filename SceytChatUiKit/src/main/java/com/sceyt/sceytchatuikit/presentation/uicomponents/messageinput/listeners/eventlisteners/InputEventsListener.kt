package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners

import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState

sealed interface InputEventsListener {

    fun interface StateListener : InputEventsListener {
        fun onStateChanged(state: InputState)
    }

    /** Use this if you want to implement all callbacks */
    interface EventListeners : StateListener
}