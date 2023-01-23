package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun View.applySystemWindowInsetsPadding(
        applyLeft: Boolean = false,
        applyTop: Boolean = false,
        applyRight: Boolean = false,
        applyBottom: Boolean = false,
) {
    doOnApplyWindowInsets { view, insets, padding, _ ->
        val left = if (applyLeft) insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).left else 0
        val top = if (applyTop) insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).top else 0
        val right = if (applyRight) insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).right else 0
        val bottom = if (applyBottom) insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).bottom else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

fun View.applySystemWindowInsetsMargin(
        applyLeft: Boolean = false,
        applyTop: Boolean = false,
        applyRight: Boolean = false,
        applyBottom: Boolean = false,
) {
    doOnApplyWindowInsets { view, insets, _, margin ->
        val left = if (applyLeft) insets.getInsets(WindowInsetsCompat.Type.systemBars()).left else 0
        val top = if (applyTop) insets.getInsets(WindowInsetsCompat.Type.systemBars()).top else 0
        val right = if (applyRight) insets.getInsets(WindowInsetsCompat.Type.systemBars()).right else 0
        val bottom = if (applyBottom) insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom else 0

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = margin.left + left
            topMargin = margin.top + top
            rightMargin = margin.right + right
            bottomMargin = margin.bottom + bottom
        }
    }
}

fun View.doOnApplyWindowInsets(
        block: (View, WindowInsetsCompat, InitialPadding, InitialMargin) -> Unit,
) {
    // Create a snapshot of the view's padding & margin states
    val initialPadding = recordInitialPaddingForView(this)
    val initialMargin = recordInitialMarginForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding & margin states
    setOnApplyWindowInsetsListener { v, insets ->
        block(v, WindowInsetsCompat.toWindowInsetsCompat(insets), initialPadding, initialMargin)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

class InitialMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

private fun recordInitialMarginForView(view: View): InitialMargin {
    val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            ?: throw IllegalArgumentException("Invalid view layout params")
    return InitialMargin(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}



