package com.sceyt.chatuikit.presentation.components.channel.input.listeners.event

import android.widget.ImageView
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

open class InputEventsListenerImpl : InputEventsListener.InputEventListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessageInputView) {
        defaultListeners = view
    }

    private var defaultListeners: InputEventsListener.InputEventListeners? = null
    private var inputStateListener: InputEventsListener.InputStateListener? = null
    private var mentionListener: InputEventsListener.MentionUsersListener? = null
    private var multiselectModeListener: InputEventsListener.MultiselectModeListener? = null
    private var searchModeListener: InputEventsListener.SearchModeListener? = null

    override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
        defaultListeners?.onInputStateChanged(sendImage, state)
        inputStateListener?.onInputStateChanged(sendImage, state)
    }

    override fun onMentionUsersListener(query: String) {
        defaultListeners?.onMentionUsersListener(query)
        mentionListener?.onMentionUsersListener(query)
    }

    override fun onMultiselectModeListener(isMultiselectMode: Boolean) {
        defaultListeners?.onMultiselectModeListener(isMultiselectMode)
        multiselectModeListener?.onMultiselectModeListener(isMultiselectMode)
    }

    override fun onSearchModeChangeListener(inSearchMode: Boolean) {
        defaultListeners?.onSearchModeChangeListener(inSearchMode)
        searchModeListener?.onSearchModeChangeListener(inSearchMode)
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

    internal fun withDefaultListeners(
            listener: InputEventsListener.InputEventListeners
    ): InputEventsListenerImpl {
        defaultListeners = listener
        return this
    }
}