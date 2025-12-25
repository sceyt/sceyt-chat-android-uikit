package com.sceyt.chatuikit.presentation.components.channel.input.style

import androidx.annotation.ColorInt
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.presentation.components.channel.input.animation.InputActionContainerAnimator
import com.sceyt.chatuikit.presentation.components.channel.input.animation.SlideInputActionAnimator
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

/**
 * Style configuration for actions at a specific position (LEADING or TRAILING).
 *
 * @property backgroundColor Background color for the actions container
 * @property iconSize Size of action icons in pixels
 * @property iconTint Tint color for enabled action icons
 * @property spacing Spacing between action icons in pixels
 * @property animationDuration Duration of add/remove animations in milliseconds
 * @property containerAnimation Custom animator for show/hide animations of the entire container
 */
data class InputActionStyle(
    @param:ColorInt val backgroundColor: Int = UNSET_COLOR,
    @param:ColorInt val iconTint: Int = UNSET_COLOR,
    val iconSize: Int = 26.dpToPx(),
    val spacing: Int = 8.dpToPx(),
    val animationDuration: Long = 200,
    val containerAnimation: InputActionContainerAnimator? = SlideInputActionAnimator(),
)

