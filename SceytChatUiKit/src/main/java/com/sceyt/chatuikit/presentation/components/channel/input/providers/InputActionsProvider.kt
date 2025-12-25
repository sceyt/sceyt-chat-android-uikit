package com.sceyt.chatuikit.presentation.components.channel.input.providers

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

/**
 * Provider for input actions inside MessageInputView.
 * Implement this interface to provide custom actions based on input state.
 *
 * Example:
 * ```
 * class MyActionsProvider : InputActionsProvider {
 *     override fun getActions(context: Context, inputState: InputState): List<InputAction> {
 *         return when (inputState) {
 *             is InputState.Text -> listOf(emojiAction, mentionAction)
 *             is InputState.Voice -> listOf(micAction)
 *             else -> emptyList()
 *         }
 *     }
 * }
 * ```
 */
interface InputActionsProvider {
    /**
     * Returns list of actions to display based on current input state.
     *
     * @param context Android context
     * @param inputState Current state of the input (Text, Voice, etc.)
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

