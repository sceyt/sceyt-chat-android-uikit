package com.sceyt.chat.ui.utils

import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter

object BindingUtil {

    @BindingAdapter("bind:setChannelTitleText")
    @JvmStatic
    fun setChannelTitleText(tv: AppCompatTextView, int: Boolean?) {
        /* with(tv) {
             setTextColor(ContextCompat.getColor(tv.context, ChannelConfig.titleColor))
           //  textSize = resources.getDimensionPixelSize(ChannelConfig.titleSize).toFloat()
         }*/
    }
}