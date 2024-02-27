package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners

import android.widget.ImageView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView

open class InputEventsListenerImpl(view: MessageInputView) : InputEventsListener.InputEventListeners {
    private var defaultListeners: InputEventsListener.InputEventListeners = view
    private var inputStateListener: InputEventsListener.InputStateListener? = null
    private var mentionListener: InputEventsListener.MentionUsersListener? = null
    private var multiselectModeListener: InputEventsListener.MultiselectModeListener? = null
    private var searchModeListener: InputEventsListener.SearchModeListener? = null

    override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
        defaultListeners.onInputStateChanged(sendImage, state)
        inputStateListener?.onInputStateChanged(sendImage, state)
    }

    override fun onMentionUsersListener(query: String) {
        defaultListeners.onMentionUsersListener(query)
        mentionListener?.onMentionUsersListener(query)
    }

    override fun onMultiselectModeListener(isMultiselectMode: Boolean) {
        defaultListeners.onMultiselectModeListener(isMultiselectMode)
        multiselectModeListener?.onMultiselectModeListener(isMultiselectMode)
    }

    override fun onSearchModeListener(inSearchMode: Boolean) {
        defaultListeners.onSearchModeListener(inSearchMode)
        searchModeListener?.onSearchModeListener(inSearchMode)
    }

    fun setListener(listener: InputEventsListener) {
        when (listener) {
            is InputEventsListener.InputEventListeners -> {
                inputStateListener = listener
                mentionListener = listener
                multiselectModeListener = listener
                searchModeListener = listener
            }

            is InputEventsListener.InputStateListener -> {
                inputStateListener = listener
            }

            is InputEventsListener.MentionUsersListener -> {
                mentionListener = listener
            }

            is InputEventsListener.MultiselectModeListener -> {
                multiselectModeListener = listener
            }

            is InputEventsListener.SearchModeListener -> {
                searchModeListener = listener
            }
        }
    }
}