package com.sceyt.chatuikit.extensions

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun Activity.applyInsetsAndWindowColor(
        rootView: View,
        applyTopInsets: Boolean = true,
        applyBottomInsets: Boolean = true,
        applyLeftInsets: Boolean = true,
        applyRightInsets: Boolean = true,
        applyLandscapeRoundedCorners: Boolean = true,
        windowColor: Int = Color.BLACK
) {
    rootView.applyInsets(
        applyTopInsets = applyTopInsets,
        applyBottomInsets = applyBottomInsets,
        applyLeftInsets = applyLeftInsets,
        applyRightInsets = applyRightInsets,
        applyLandscapeRoundedCorners = applyLandscapeRoundedCorners
    ) { _, _, _, _ ->
        window.setBackgroundDrawable(windowColor.toDrawable())
    }
}


fun View.applyInsets(
        applyTopInsets: Boolean = true,
        applyBottomInsets: Boolean = true,
        applyLeftInsets: Boolean = true,
        applyRightInsets: Boolean = true,
        applyLandscapeRoundedCorners: Boolean = true,
        onAppliedRoundedCorners: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
) = ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout())

    val left = if (applyLeftInsets) insets.left else 0
    val top = if (applyTopInsets) insets.top else 0
    val right = if (applyRightInsets) insets.right else 0
    val bottom = if (applyBottomInsets) insets.bottom else 0

    if (left == 0 && top == 0 && right == 0 && bottom == 0)
        return@setOnApplyWindowInsetsListener windowInsets

    if (applyTopInsets || applyBottomInsets)
        view.setPaddings(top = top, bottom = bottom)

    if (applyLeftInsets || applyRightInsets)
        view.setMargins(left = left, right = right)

    if (context.isLandscape() && applyLandscapeRoundedCorners && SDK_INT >= Build.VERSION_CODES.S) {
        view.applyRoundedCorners(windowInsets.toWindowInsets())
        onAppliedRoundedCorners(left, top, right, bottom)
    }
    windowInsets
}

@RequiresApi(Build.VERSION_CODES.S)
fun View.applyRoundedCorners(insets: WindowInsets?) {
    val radiusTopLeft = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat()
            ?: 0f
    val radiusBottomLeft = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius?.toFloat()
            ?: 0f
    if (radiusTopLeft == 0f && radiusBottomLeft == 0f) return

    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, outline: Outline) {
            if (v.width == 0 || v.height == 0) return
            val path = Path().apply {
                moveTo(0f, radiusTopLeft)
                quadTo(0f, 0f, radiusTopLeft, 0f) // top-left corner

                lineTo(v.width.toFloat(), 0f)
                lineTo(v.width.toFloat(), v.height.toFloat())

                lineTo(radiusBottomLeft, v.height.toFloat())
                quadTo(0f, v.height.toFloat(), 0f, v.height - radiusBottomLeft) // bottom-left corner
                close()
            }

            outline.setPath(path)
        }
    }
}

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
        userDefaultMargins: Boolean = true
) {
    doOnApplyWindowInsets { view, insets, _, margin ->
        val left = if (applyLeft) insets.getInsets(WindowInsetsCompat.Type.systemBars()).left else 0
        val top = if (applyTop) insets.getInsets(WindowInsetsCompat.Type.systemBars()).top else 0
        val right = if (applyRight) insets.getInsets(WindowInsetsCompat.Type.systemBars()).right else 0
        val bottom = if (applyBottom) insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom else 0

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = if (userDefaultMargins) margin.left else 0 + left
            topMargin = if (userDefaultMargins) margin.top else 0 + top
            rightMargin = if (userDefaultMargins) margin.right else 0 + right
            bottomMargin = if (userDefaultMargins) margin.bottom else 0 + bottom
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



