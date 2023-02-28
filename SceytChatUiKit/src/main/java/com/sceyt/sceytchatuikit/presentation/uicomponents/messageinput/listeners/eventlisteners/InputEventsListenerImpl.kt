package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners

import android.widget.ImageView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView

open class InputEventsListenerImpl(view: MessageInputView) : InputEventsListener.InputEventListeners {
    private var defaultListeners: InputEventsListener.InputEventListeners = view
    private var inputStateListener: InputEventsListener.InputStateListener? = null
    private var mentionListener: InputEventsListener.MentionUsersListener? = null

    override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
        defaultListeners.onInputStateChanged(sendImage, state)
        inputStateListener?.onInputStateChanged(sendImage, state)
    }

    override fun onMentionUsersListener(query: String) {
        defaultListeners.onMentionUsersListener(query)
        mentionListener?.onMentionUsersListener(query)
    }

    fun setListener(listener: InputEventsListener) {
        when (listener) {
            is InputEventsListener.InputEventListeners -> {
                inputStateListener = listener
                mentionListener = listener
            }
            is InputEventsListener.InputStateListener -> {
                inputStateListener = listener
            }
            is InputEventsListener.MentionUsersListener -> {
                mentionListener = listener
            }
        }
    }
}