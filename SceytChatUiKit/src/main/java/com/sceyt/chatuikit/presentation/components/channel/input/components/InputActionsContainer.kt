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

    private var leadingStyle: InputActionStyle? = null
    private var trailingStyle: InputActionStyle? = null

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

        // Hide/show with animation, clear ONLY after hide animation ends
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
        leadingActionsView.applyStyle(style = style.leadingActionsStyle)
        trailingActionsView.applyStyle(style = style.trailingActionsStyle)
    }

    private fun updateVisibility(
        position: ActionsPosition,
        visible: Boolean,
        onEnd: () -> Unit = {}
    ) {
        val (actionsView, style) = when (position) {
            ActionsPosition.LEADING -> leadingActionsView to leadingStyle
            ActionsPosition.TRAILING -> trailingActionsView to trailingStyle
        }

        // No-op if state already matches
        if (actionsView.isVisible == visible) return

        val animation = style?.containerAnimation
        val canAnimate = animation != null && actionsView.isAttachedToWindow

        if (canAnimate) {
            actionsView.disableLayoutTransition()

            val endCallback = {
                onEnd()
                actionsView.restoreLayoutTransition()
            }

            if (visible) {
                animation.animateShow(actionsView, position, endCallback)
            } else {
                animation.animateHide(actionsView, position, endCallback)
            }
        } else {
            actionsView.isVisible = visible
            onEnd()
        }
    }
}
