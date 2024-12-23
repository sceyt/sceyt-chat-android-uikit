package com.sceyt.chatuikit.presentation.components.channel.input.listeners.event

import android.widget.ImageView
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

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

    fun interface SearchModeListener : InputEventsListener {
        fun onSearchModeChangeListener(inSearchMode: Boolean)
    }

    /** Use this if you want to implement all callbacks */
    interface InputEventListeners : InputStateListener, MentionUsersListener,
            MultiselectModeListener, SearchModeListener
}

internal fun InputEventsListener.setListener(listener: InputEventsListener) {
    (this as? InputEventsListenerImpl)?.setListener(listener)
}