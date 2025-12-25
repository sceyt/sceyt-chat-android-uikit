package com.sceyt.chatuikit.presentation.components.channel.input.components

import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.channels.ActionsPosition
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.presentation.components.channel.input.style.InputActionStyle
import com.sceyt.chatuikit.presentation.components.channel.input.style.InputActionsStyle

/**
 * Container that manages InputActionsViews for leading and trailing actions.
 * Handles visibility, display modes, and action updates.
 */
class InputActionsContainer(
    private val leadingActionsView: InputActionsView,
    private val trailingActionsView: InputActionsView
) {

    private var leadingStyle: InputActionStyle = InputActionStyle()
    private var trailingStyle: InputActionStyle = InputActionStyle()

    /**
     * Sets actions, automatically filtering by position.
     * Efficiently updates only changed actions (add/remove/update) while maintaining order.
     */
    fun setActions(actions: List<InputAction>) {
        val leadingActions = actions.filter {
            it.position == ActionsPosition.LEADING && it.isVisible
        }

        val trailingActions = actions.filter {
            it.position == ActionsPosition.TRAILING && it.isVisible
        }

        // Set actions with proper ordering and diffing
        if (leadingActions.isNotEmpty())
            leadingActionsView.setActions(leadingActions)

        if (trailingActions.isNotEmpty())
            trailingActionsView.setActions(trailingActions)

        // Update visibility with container animations
        updateVisibility(ActionsPosition.LEADING, leadingActions.isNotEmpty()) {
            if (leadingActions.isEmpty()) {
                leadingActionsView.clearActions()
            }
        }
        updateVisibility(ActionsPosition.TRAILING, trailingActions.isNotEmpty()) {
            if (trailingActions.isEmpty()) {
                trailingActionsView.clearActions()
            }
        }
    }


    /**
     * Sets the style for a specific position.
     */
    fun setStyle(style: InputActionsStyle) {
        leadingStyle = style.leadingActionsStyle
        trailingStyle = style.trailingActionsStyle
        leadingActionsView.applyStyle(style = leadingStyle)
        trailingActionsView.applyStyle(style = trailingStyle)
    }

    private fun updateVisibility(
        position: ActionsPosition,
        visible: Boolean,
        onEnd: () -> Unit = { }
    ) {
        // Determine which view and style to use
        val actionsView: InputActionsView
        val style: InputActionStyle

        when (position) {
            ActionsPosition.LEADING -> {
                actionsView = leadingActionsView
                style = leadingStyle
            }

            ActionsPosition.TRAILING -> {
                actionsView = trailingActionsView
                style = trailingStyle
            }
        }

        if (visible) {
            // Show animation
            if (!actionsView.isVisible) {
                if (style.containerAnimation != null && actionsView.isAttachedToWindow) {
                    // Disable LayoutTransition to avoid conflict with custom animation
                    actionsView.disableLayoutTransition()

                    style.containerAnimation.animateShow(actionsView, position) {
                        // Re-enable LayoutTransition after animation completes
                        onEnd()
                        actionsView.restoreLayoutTransition()
                    }
                } else {
                    // Use LayoutTransition for automatic animation
                    actionsView.isVisible = true
                    onEnd()
                }
            }
        } else {
            // Hide animation
            if (actionsView.isVisible) {
                if (style.containerAnimation != null && actionsView.isAttachedToWindow) {
                    // Disable LayoutTransition to avoid conflict with custom animation
                    actionsView.disableLayoutTransition()

                    style.containerAnimation.animateHide(actionsView, position) {
                        // Re-enable LayoutTransition after animation completes
                        onEnd()
                        actionsView.restoreLayoutTransition()
                    }
                } else {
                    // Use LayoutTransition for automatic animation
                    actionsView.isVisible = false
                    onEnd()
                }
            }
        }
    }
}
