package com.sceyt.chatuikit.presentation.components.channel.input.providers

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

/**
 * Provider for input actions inside MessageInputView.
 * Implement this interface to provide custom actions based on input state.
 *
 * @see CompositeInputActionsProvider
 */
interface InputActionsProvider {
    /**
     * Returns list of actions to display based on current input state.
     *
     * @param context Android context
     * @param inputState Current state of the input (Voice, Recording, Text, TextWithAttachments, Attachments)
     * @return List of actions to display
     */
    fun getActions(context: Context, inputState: InputState): List<InputAction>

    /**
     * Called when an action is clicked.
     * Optional override - default implementation does nothing.
     *
     * @param action The action that was clicked
     */
    fun onActionClick(action: InputAction) {}
}

