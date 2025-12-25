package com.sceyt.chatuikit.presentation.components.channel.input.style

/**
 * Style configuration for input actions.
 * Contains separate styles for leading and trailing actions.
 */
data class InputActionsStyle(
    val leadingActionsStyle: InputActionStyle = InputActionStyle(),
    val trailingActionsStyle: InputActionStyle = InputActionStyle()
)

