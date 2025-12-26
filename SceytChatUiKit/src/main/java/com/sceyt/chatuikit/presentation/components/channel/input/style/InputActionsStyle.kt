package com.sceyt.chatuikit.presentation.components.channel.input.style

import android.content.Context
import android.content.res.TypedArray
import com.sceyt.chatuikit.styles.StyleCustomizer

/**
 * Style configuration for input actions.
 * Contains separate styles for leading and trailing actions.
 */
data class InputActionsStyle(
    val leadingActionsStyle: InputActionStyle,
    val trailingActionsStyle: InputActionStyle
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<InputActionsStyle> { _, style -> style }
    }

    /**
     * Builder for creating InputActionsStyle from XML attributes.
     */
    class Builder(
        private val context: Context,
        private val array: TypedArray
    ) {
        fun build(): InputActionsStyle {
            val builder = InputActionStyle.Builder(context, array)
            val leadingStyle = builder.buildForLeading()
            val trailingStyle = builder.buildForTrailing()

            return InputActionsStyle(
                leadingActionsStyle = leadingStyle,
                trailingActionsStyle = trailingStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
