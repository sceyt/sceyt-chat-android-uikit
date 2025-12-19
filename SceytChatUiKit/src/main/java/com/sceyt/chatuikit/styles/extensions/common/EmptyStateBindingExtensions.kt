package com.sceyt.chatuikit.styles.extensions.common

import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytEmptyStateBinding
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.EmptyStateStyle

fun SceytEmptyStateBinding.applyStyle(
    style: EmptyStateStyle
) {
    icon.isVisible = style.icon != null
    if (style.icon != null) {
        icon.setImageDrawable(style.icon)

        if (style.iconTint != UNSET_COLOR) {
            icon.setColorFilter(style.iconTint)
        } else {
            icon.clearColorFilter()
        }
    }

    title.text = style.titleText
    style.titleStyle.apply(title)

    subtitle.isVisible = !style.subtitleText.isNullOrEmpty()
    if (!style.subtitleText.isNullOrEmpty()) {
        subtitle.text = style.subtitleText
        style.subtitleStyle.apply(subtitle)
    }
}