package com.sceyt.chatuikit.presentation.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getString

class PlayPauseImage @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var playIcon = context.getCompatDrawable(R.drawable.sceyt_ic_play)
    private var pauseIcon = context.getCompatDrawable(R.drawable.sceyt_ic_pause)

    @SuppressLint("PrivateResource")
    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        when (contentDescription) {
            getString(androidx.media3.ui.R.string.exo_controls_play_description) -> setImageDrawable(playIcon)
            getString(androidx.media3.ui.R.string.exo_controls_pause_description) -> setImageDrawable(pauseIcon)
        }
    }

    fun setPlayIcon(playIcon: Drawable?) {
        this.playIcon = playIcon
    }

    fun setPauseIcon(pauseIcon: Drawable?) {
        this.pauseIcon = pauseIcon
    }
}