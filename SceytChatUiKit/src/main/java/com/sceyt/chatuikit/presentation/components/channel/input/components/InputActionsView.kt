package com.sceyt.chatuikit.presentation.components.channel.input.components

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setPadding
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getSelectableItemBackground
import com.sceyt.chatuikit.extensions.setTint
import com.sceyt.chatuikit.presentation.components.channel.input.style.InputActionStyle
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

/**
 * Simple horizontal LinearLayout for displaying input actions.
 * Uses LayoutTransition for automatic smooth width animations.
 */
class InputActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val defaultTransition = LayoutTransition().apply {
        enableTransitionType(LayoutTransition.APPEARING)
    }

    private val actionViews = LinkedHashMap<String, ImageView>()
    private var actionSize = 28.dpToPx()
    private var actionSpacing = 8.dpToPx()
    private var iconTint = UNSET_COLOR

    init {
        orientation = HORIZONTAL
        layoutDirection = LAYOUT_DIRECTION_LOCALE
    }


    /**
     * Removes an action by ID
     */
    fun removeAction(actionId: String) {
        val imageView = actionViews[actionId] ?: return
        actionViews.remove(actionId)
        removeView(imageView)
    }

    /**
     * Updates an existing action
     */
    fun updateAction(action: InputAction) {
        val imageView = actionViews[action.id] ?: return
        imageView.apply {
            setImageDrawable(action.icon)
            isEnabled = action.isEnabled
            alpha = if (action.isEnabled) 1.0f else 0.5f
            setOnClickListener {
                if (action.isEnabled) {
                    action.onClick(action)
                }
            }
        }
    }

    /**
     * Clears all actions
     */
    fun clearActions() {
        actionViews.clear()
        removeAllViews()
    }

    /**
     * Sets all actions at once with smart diff logic.
     * Only adds/removes/updates what changed.
     */
    fun setActions(actions: List<InputAction>) {
        val currentIds = actionViews.keys.toSet()
        val newIds = actions.map { it.id }.toSet()

        // Remove actions that are no longer in the list
        val toRemove = currentIds - newIds
        toRemove.forEach { removeAction(it) }

        // Add new actions or update existing ones (maintaining order)
        actions.forEachIndexed { index, action ->
            if (actionViews.containsKey(action.id)) {
                // Update existing action
                updateAction(action)

                // Ensure correct order
                val currentIndex = indexOfChild(actionViews[action.id])
                if (currentIndex != index && currentIndex != -1) {
                    val view = actionViews[action.id]
                    removeView(view)
                    addView(view, index)
                }
            } else {
                // Add new action at correct position
                val imageView = createActionImageView(action, index > 0)
                actionViews[action.id] = imageView
                addView(imageView, index)
            }
        }
    }

    /**
     * Temporarily disables LayoutTransition to avoid conflicts with custom animations.
     * Returns the saved LayoutTransition to restore later.
     */
    fun disableLayoutTransition() {
        layoutTransition = null
    }

    /**
     * Restores a previously saved LayoutTransition.
     */
    fun restoreLayoutTransition() {
        layoutTransition = defaultTransition
    }

    fun applyStyle(style: InputActionStyle) {
        actionSize = style.iconSize
        actionSpacing = style.spacing
        defaultTransition.setDuration(style.animationDuration)

        if (style.backgroundColor != UNSET_COLOR) {
            setBackgroundColor(style.backgroundColor)
        }

        actionViews.values.forEachIndexed { index, imageView ->
            val params = imageView.layoutParams
            params.width = actionSize
            params.height = actionSize

            (imageView.layoutParams as? MarginLayoutParams)?.let { marginLayoutParams ->
                if (index > 0) {
                    marginLayoutParams.marginStart = actionSpacing
                }
            }

            imageView.layoutParams = params
        }
    }

    private fun createActionImageView(action: InputAction, applySpacingStart: Boolean): ImageView {
        return AppCompatImageView(context).apply {
            layoutParams = LayoutParams(actionSize, actionSize).apply {
                if (actionViews.isNotEmpty()) {
                    if (applySpacingStart)
                        marginStart = actionSpacing
                    else marginEnd = actionSpacing
                }
            }

            setImageDrawable(action.icon)

            isEnabled = action.isEnabled
            alpha = if (action.isEnabled) 1.0f else 0.5f
            background = context.getSelectableItemBackground()
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(2.dpToPx())

            if (iconTint != UNSET_COLOR)
                setTint(iconTint)

            setOnClickListener {
                if (action.isEnabled) {
                    action.onClick(action)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restoreLayoutTransition()
    }
}
