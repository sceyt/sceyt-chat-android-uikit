package com.sceyt.chatuikit.data.models.channels

import android.graphics.drawable.Drawable

/**
 * Represents an action that can be displayed in MessageInputView.
 * Actions are displayed inside the input field at LEADING or TRAILING positions.
 *
 * @property id Unique identifier for the action
 * @property position Position of the action (LEADING or TRAILING)
 * @property icon Drawable icon for the action
 * @property isEnabled Whether the action is enabled and clickable
 * @property isVisible Whether the action is visible
 * @property onClick Callback invoked when the action is clicked
 */
data class InputAction(
    val id: String,
    val position: ActionsPosition,
    val icon: Drawable?,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val onClick: (InputAction) -> Unit
)

