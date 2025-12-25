package com.sceyt.chatuikit.presentation.components.channel.input.style

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.presentation.components.channel.input.animation.InputActionContainerAnimator
import com.sceyt.chatuikit.presentation.components.channel.input.animation.SlideInputActionAnimator
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

/**
 * Style configuration for actions at a specific position (LEADING or TRAILING).
 *
 * @property backgroundColor Background color for the actions container. Default is UNSET_COLOR (no color).
 * @property iconSize Size of action icons in pixels. Default is 26dp.
 * @property iconTint Tint color for enabled action icons. Default is UNSET_COLOR (no tint).
 * @property spacing Spacing between action icons in pixels. Default is 8dp.
 * @property animationDuration Duration of add/remove animations in milliseconds. Default is 200ms.
 * @property containerAnimation Custom animator for show/hide animations of the entire container. If null, no animation is applied. Default is [SlideInputActionAnimator].
 */
data class InputActionStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val iconTint: Int,
    val iconSize: Int,
    val spacing: Int,
    val animationDuration: Long,
    val containerAnimation: InputActionContainerAnimator?,
) {

    internal class Builder(
        @Suppress("unused") private val context: Context,
        private val typedArray: TypedArray
    ) {
        private val defaultIconSize = 26.dpToPx()
        private val defaultSpacing = 8.dpToPx()
        private val defaultAnimationDuration = 200
        private var defaultContainerAnimation = SlideInputActionAnimator()

        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var iconTint: Int = UNSET_COLOR

        private var iconSize: Int = defaultIconSize
        private var spacing: Int = defaultSpacing
        private var animationDuration: Long = defaultAnimationDuration.toLong()

        fun buildForLeading(): InputActionStyle {
            backgroundColor = typedArray.getColor(
                R.styleable.MessageInputView_sceytUiInputLeadingActionsBackgroundColor,
                UNSET_COLOR
            )

            iconTint = typedArray.getColor(
                R.styleable.MessageInputView_sceytUiInputLeadingActionsIconTint,
                UNSET_COLOR
            )

            iconSize = typedArray.getDimensionPixelSize(
                R.styleable.MessageInputView_sceytUiInputLeadingActionsIconSize,
                 defaultIconSize
            )

            spacing = typedArray.getDimensionPixelSize(
                R.styleable.MessageInputView_sceytUiInputLeadingActionsSpacing,
                defaultSpacing
            )

            animationDuration = typedArray.getInt(
                R.styleable.MessageInputView_sceytUiInputLeadingActionsAnimationDuration,
                defaultAnimationDuration
            ).toLong()

            return InputActionStyle(
                backgroundColor = backgroundColor,
                iconTint = iconTint,
                iconSize = iconSize,
                spacing = spacing,
                animationDuration = animationDuration,
                containerAnimation = defaultContainerAnimation
            )
        }

        fun buildForTrailing(): InputActionStyle {
            backgroundColor = typedArray.getColor(
                R.styleable.MessageInputView_sceytUiInputTrailingActionsBackgroundColor,
                UNSET_COLOR
            )

            iconTint = typedArray.getColor(
                R.styleable.MessageInputView_sceytUiInputTrailingActionsIconTint,
                UNSET_COLOR
            )

            iconSize = typedArray.getDimensionPixelSize(
                R.styleable.MessageInputView_sceytUiInputTrailingActionsIconSize,
                defaultIconSize
            )

            spacing = typedArray.getDimensionPixelSize(
                R.styleable.MessageInputView_sceytUiInputTrailingActionsSpacing,
                defaultSpacing
            )

            animationDuration = typedArray.getInt(
                R.styleable.MessageInputView_sceytUiInputTrailingActionsAnimationDuration,
                defaultAnimationDuration
            ).toLong()

            return InputActionStyle(
                backgroundColor = backgroundColor,
                iconTint = iconTint,
                iconSize = iconSize,
                spacing = spacing,
                animationDuration = animationDuration,
                containerAnimation = defaultContainerAnimation
            )
        }
    }
}
