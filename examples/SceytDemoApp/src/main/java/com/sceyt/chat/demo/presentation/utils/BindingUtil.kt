package com.sceyt.chat.demo.presentation.utils

import android.view.View
import androidx.databinding.BindingAdapter

object BindingUtil {
    @BindingAdapter("visibleIf")
    @JvmStatic
    fun visibleIf(anyView: View, show: Boolean) {
        anyView.visibility = if (show) View.VISIBLE else View.GONE
    }
}