package com.sceyt.chatuikit.presentation.common

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.sceyt.chatuikit.extensions.getCompatColor

object BindingUtil {

    @BindingAdapter("setTextColor")
    @JvmStatic
    fun setTextColor(textView: TextView,  color: Int) {
        textView.setTextColor(textView.context.getCompatColor(color))
    }

    @BindingAdapter("setTintColor")
    @JvmStatic
    fun setTintColor(imageView: ImageView, color: Int) {
        imageView.setColorFilter(imageView.context.getCompatColor(color))
    }
}