package com.sceyt.chatuikit.presentation.components.channel.input.animation

import android.view.View
import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.channels.ActionsPosition

/**
 * Default implementation that slides and fades the actions container in/out.
 */
class SlideInputActionAnimator(
    private val duration: Long = 200,
    private val translationDistance: Float = 50f
) : InputActionContainerAnimator {

    override fun animateShow(view: View, position: ActionsPosition, onEnd: () -> Unit) {
        view.alpha = 0f
        view.translationX = if (position == ActionsPosition.LEADING) -translationDistance else translationDistance
        view.isVisible = true

        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .withEndAction(onEnd)
            .start()
    }

    override fun animateHide(view: View, position: ActionsPosition, onEnd: () -> Unit) {
        view.animate()
            .alpha(0f)
            .translationX(if (position == ActionsPosition.LEADING) -translationDistance else translationDistance)
            .setDuration(duration)
            .withEndAction {
                view.isVisible = false
                view.alpha = 1f
                view.translationX = 0f
                onEnd()
            }
            .start()
    }
}

