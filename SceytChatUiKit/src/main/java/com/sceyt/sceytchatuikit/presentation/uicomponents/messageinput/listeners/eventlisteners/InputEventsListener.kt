package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners

import android.widget.ImageView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState

sealed interface InputEventsListener {

    fun interface InputStateListener : InputEventsListener {
        fun onInputStateChanged(sendImage: ImageView, state: InputState)
    }

    fun interface MentionUsersListener : InputEventsListener {
        fun onMentionUsersListener(query: String)
    }

    fun interface MultiselectModeListener : InputEventsListener {
        fun onMultiselectModeListener(isMultiselectMode: Boolean)
    }

    /** Use this if you want to implement all callbacks */
    interface InputEventListeners : InputStateListener, MentionUsersListener, MultiselectModeListener
}