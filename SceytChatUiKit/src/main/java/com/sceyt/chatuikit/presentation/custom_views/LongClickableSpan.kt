package com.sceyt.chatuikit.presentation.custom_views

import android.view.View

/**
 * Interface for ClickableSpan implementations that want to handle long-press events.
 * Implement this interface in your custom ClickableSpan to provide long-press behavior.
 */
interface LongClickableSpan {
    /**
     * Called when the span is long-pressed.
     * @param widget The view that was long-pressed
     * @return true if the long-press was handled, false otherwise
     */
    fun onLongClick(widget: View): Boolean
}