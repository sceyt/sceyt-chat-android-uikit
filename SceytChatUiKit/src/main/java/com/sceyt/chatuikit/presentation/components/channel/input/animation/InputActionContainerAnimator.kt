package com.sceyt.chatuikit.presentation.components.channel.input.animation

import android.view.View
import com.sceyt.chatuikit.data.models.channels.ActionsPosition

/**
 * Interface for customizing show/hide animations of the entire actions container.
 * Implement this interface to create custom animations for when the actions view appears or disappears.
 */
interface InputActionContainerAnimator {
    
    /**
     * Animates showing the actions container.
     * @param view The actions view to animate
     * @param position The position of the actions (LEADING or TRAILING)
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateShow(view: View, position: ActionsPosition, onEnd: () -> Unit = {})
    
    /**
     * Animates hiding the actions container.
     * @param view The actions view to animate
     * @param position The position of the actions (LEADING or TRAILING)
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateHide(view: View, position: ActionsPosition, onEnd: () -> Unit = {})
}

