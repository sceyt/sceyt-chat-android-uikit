package com.sceyt.chatuikit.presentation.common

import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.SimpleColorFilter
import com.sceyt.chatuikit.extensions.getCompatColor

object BindingUtil {

    @BindingAdapter("setTextColor")
    @JvmStatic
    fun setTextColor(textView: TextView, colorId: Int) {
        textView.setTextColor(textView.context.getCompatColor(colorId))
    }

    @BindingAdapter("setTintColor")
    @JvmStatic
    fun setTintColor(imageView: ImageView, colorId: Int) {
        imageView.setColorFilter(imageView.context.getCompatColor(colorId))
    }

    @BindingAdapter("setProgressColor")
    @JvmStatic
    fun setProgressColor(progressBar: ProgressBar, colorId: Int) {
        progressBar.indeterminateDrawable.colorFilter = SimpleColorFilter(progressBar.context.getCompatColor(colorId))
    }
}