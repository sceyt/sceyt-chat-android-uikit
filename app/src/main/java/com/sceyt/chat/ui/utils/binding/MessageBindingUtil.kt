package com.sceyt.chat.ui.utils.binding

import androidx.databinding.BindingAdapter
import com.sceyt.chat.ui.presentation.customviews.SceytVideoControllerView

object MessageBindingUtil {

    @BindingAdapter("bind:showPlayPauseButton")
    @JvmStatic
    fun showPlayPauseButton(view: SceytVideoControllerView, show: Boolean) {
        view.showPlayPauseButtons(show)
    }
}