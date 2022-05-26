package com.sceyt.chat.ui.utils.binding

import androidx.databinding.BindingAdapter
import com.sceyt.chat.ui.presentation.customviews.VideoControllerView

object MessageBindingUtil {

    @BindingAdapter("bind:showPlayPauseButton")
    @JvmStatic
    fun showPlayPauseButton(view: VideoControllerView, show: Boolean) {
        view.showPlayPauseButtons(show)
    }
}